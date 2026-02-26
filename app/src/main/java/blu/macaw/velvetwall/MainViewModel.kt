package blu.macaw.velvetwall

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import blu.macaw.velvetwall.data.BlockedCallLog
import blu.macaw.velvetwall.data.BlockedNumber
import blu.macaw.velvetwall.data.CallRepository
import blu.macaw.velvetwall.data.UserSettings
import blu.macaw.velvetwall.data.WhiteListNumber
import blu.macaw.velvetwall.data.worker.CleanupWorker
import blu.macaw.velvetwall.utils.BillingHelper
import blu.macaw.velvetwall.utils.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

class MainViewModel(
    application: Application,
    private val repository: CallRepository,
    private val userSettings: UserSettings
) : AndroidViewModel(application) {

    private val billingHelper = BillingHelper(
        context = application,
        userSettings = userSettings,
        onSuccess = { triggerSuccess() }
    )

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    // --- ESTADOS DE FATURAMENTO (BILLING) ---

    /**
     * Observa o status Premium em tempo real para liberar recursos de elite.
     */
    val isPremiumEnabled: StateFlow<Boolean> = userSettings.isPremiumFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5000),
        initialValue = false
    )

    val showSuccess: StateFlow<Boolean> = userSettings.showSuccess
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = false
        )
    /**
     * Monitora o início da degustação para controle de Paywall.
     */
    val trialStartTimestamp: StateFlow<Long> = userSettings.trialStartFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5000),
        initialValue = 0L
    )


    init {
        // Conexão imediata com a Play Store para validação de licenças
        billingHelper.startConnection()

        billingHelper.checkExistingPurchases { isPro ->
            viewModelScope.launch {
                userSettings.savePremiumStatus(isPro) // Faz o status "grudar" no banco
            }
        }
        // Feedback visual imediato ao detectar um bloqueio no histórico
        viewModelScope.launch {
            repository.callLogs.collect { logs ->
                val lastLog = logs.firstOrNull()
                if (lastLog != null && System.currentTimeMillis() - lastLog.timestamp < 2000) {
                    notifyBlock(lastLog.number)
                }
            }
        }
    }

    /**
     * Inicia o fluxo de compra da licença vitalícia PRO.
     */
    fun buyPremium(activity: Activity) {
        billingHelper.launchPurchaseFlow(activity)
    }

    /**
     * Restaura compras anteriores, essencial para aprovação na Play Store.
     */


    // No seu MainViewModel.kt

    fun restorePremium(context: Context) {
        viewModelScope.launch {
            try {
                _isRestoring.value = true
                _blockEvent.emit("Sincronizando com a Google Play...")

                // Criamos um timer de segurança externo
                launch {
                    delay(10000) // 10 segundos de limite absoluto
                    if (_isRestoring.value) {
                        _isRestoring.value = false
                        Log.d("VELVET", "Timeout: Google não respondeu, destravando UI.")
                    }
                }

                billingHelper.queryExistingPurchases { isPro ->
                    // O callback precisa rodar no escopo da Main thread para atualizar a UI
                    viewModelScope.launch {
                        try {
                            userSettings.savePremiumStatus(isPro)
                            val msg = if (isPro) "Licença PRO ativa! 🦜" else "Nenhuma licença encontrada."
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        } finally {
                            _isRestoring.value = false // GARANTE que a rodinha pare no sucesso/erro
                        }
                    }
                }
            } catch (e: Exception) {
                _isRestoring.value = false // GARANTE que pare se o código crashar
                Log.e("VELVET", "Erro crítico no restauro: ${e.message}")
            }
        }
    }

    // --- GESTÃO DE LISTAS E HISTÓRICO ---
    val blockedDDDs = userSettings.blockedDDDsFlow.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptySet())
    val blacklist = repository.blacklist
    val whitelist = repository.whitelist
    val history = repository.callLogs

    // --- CONFIGURAÇÕES TÉCNICAS (TOGGLES) ---
    val blockPrivate = userSettings.blockPrivateFlow
    val blockUnknown = userSettings.blockUnknownFlow
    val notificationsEnabled = userSettings.notificationsFlow
    val biometricEnabled = userSettings.biometricFlow
    val nightModeEnabled = userSettings.nightModeFlow.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), true)
    val paranoidModeEnabled = userSettings.paranoidModeFlow.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), false)
    val stealthModeEnabled = userSettings.stealthModeFlow.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), false)
    val userLocalDDD = userSettings.userLocalDDDFlow.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), "")
    val cleanupDays = userSettings.cleanupDaysFlow.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), 30)

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _blockEvent = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val blockEvent = _blockEvent.asSharedFlow()

    // --- AÇÕES DE INTERFACE ---

    fun notifyBlock(number: String) {
        viewModelScope.launch {
            _blockEvent.emit("🛡️ Escudo Ativado: $number bloqueado!")
            delay(3000)
            _blockEvent.emit("")
        }
    }

    // Toggles de Persistência
    fun toggleStealthMode(enabled: Boolean) = viewModelScope.launch { userSettings.setStealthMode(enabled) }
    fun toggleParanoidMode(enabled: Boolean) = viewModelScope.launch { userSettings.setParanoidMode(enabled) }
    fun toggleNightMode(enabled: Boolean) = viewModelScope.launch { userSettings.setNightMode(enabled) }

    fun setBlockPrivate(enabled: Boolean) = viewModelScope.launch { userSettings.setBlockPrivate(enabled) }
    fun setBlockUnknown(enabled: Boolean) = viewModelScope.launch { userSettings.setBlockUnknown(enabled) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { userSettings.setNotifications(enabled) }
    fun setBiometric(enabled: Boolean) = viewModelScope.launch { userSettings.setBiometric(enabled) }

    fun setUserLocalDDD(ddd: String) {
        if (ddd.length <= 2 && ddd.all { it.isDigit() }) {
            viewModelScope.launch { userSettings.saveUserLocalDDD(ddd) }
        }
    }

    fun addBlockedDDD(ddd: String) {
        if (ddd.length == 2 && ddd.all { it.isDigit() }) viewModelScope.launch { userSettings.saveDDD(ddd) }
    }

    fun removeBlockedDDD(ddd: String) = viewModelScope.launch { userSettings.removeDDD(ddd) }

    // --- GESTÃO DE REPOSITÓRIO ---
    fun addToBlacklist(number: String, reason: String = "Manual") = viewModelScope.launch { repository.addToBlacklist(number, reason) }
    fun removeFromBlacklist(item: BlockedNumber) = viewModelScope.launch { repository.removeFromBlacklist(item) }
    fun addToWhitelist(number: String, name: String = "Autorizado") = viewModelScope.launch { repository.addToWhitelist(number, name) }
    fun removeFromWhitelist(item: WhiteListNumber) = viewModelScope.launch { repository.removeFromWhitelist(item) }

    fun allowFromHistory(item: BlockedCallLog) = viewModelScope.launch {
        repository.addToWhitelist(item.number, "Desbloqueado via Histórico")
        repository.deleteLog(item)
    }

    fun blockFromHistory(item: BlockedCallLog) = viewModelScope.launch {
        repository.addToBlacklist(item.number, "Confirmado via Histórico")
    }

    fun clearEverything() = viewModelScope.launch {
        repository.clearLogs()
        val nm = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
    }

    // --- SISTEMA E BACKGROUND ---

    fun checkRoleStatus(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            _isServiceActive.value = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else {
            _isServiceActive.value = true
        }
    }

    /**
     * Agenda a limpeza automática de logs, mantendo o app leve e performático.
     */
    fun updateCleanupSettings(days: Int) = viewModelScope.launch {
        userSettings.setCleanupDays(days)
        val constraints = Constraints.Builder().setRequiresBatteryNotLow(true).setRequiresDeviceIdle(true).build()
        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("log_cleanup")
            .build()
        WorkManager.Companion.getInstance(getApplication()).enqueueUniquePeriodicWork("VelvetCleanup", ExistingPeriodicWorkPolicy.REPLACE, cleanupRequest)
    }

    fun triggerTestNotification() = viewModelScope.launch {
        try {
            NotificationHelper(getApplication()).showNotification(number = "+55 (11) 99999-9999", reason = "Teste Blu Macaw Lab's", isTest = true)
        } catch (e: Exception) {}
    }

    // --- O PULO DO GATO: MOVER DA BLACK PARA WHITE ---
    fun moveBlackToWhite(item: BlockedNumber) {
        viewModelScope.launch {
            // 1. Remove da Negra
            repository.removeFromBlacklist(item)
            // 2. Adiciona na Branca
            repository.addToWhitelist(item.number, "Movido da Lista Negra")
        }
    }
    fun clearHistory() {
        viewModelScope.launch { repository.clearLogs() }
    }

    private val _showSuccessScreen = MutableStateFlow(false)

    fun triggerSuccess() {
        viewModelScope.launch {
            userSettings.setShowSuccess(true) // Ativa a tela de confetes
        }
    }

    fun dismissSuccessAnimation() {
        viewModelScope.launch {
            userSettings.setShowSuccess(false) // Fecha a tela
        }
    }
}