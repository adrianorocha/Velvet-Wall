package blu.macaw.velvetwall

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import blu.macaw.velvetwall.ui.screens.PaywallPriceState
import blu.macaw.velvetwall.utils.BillingHelper
import blu.macaw.velvetwall.utils.NotificationHelper
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainViewModel(
    application: Application,
    private val repository: CallRepository,
    private val userSettings: UserSettings
) : AndroidViewModel(application) {

    // --- INSTÂNCIAS DE FATURAMENTO ---
    private val billingHelper = BillingHelper(
        context = application,
        userSettings = userSettings,
        onSuccess = { triggerSuccess() }
    )

    // 🛠️ CORREÇÃO: Variável do Google Play declarada
    private lateinit var billingClient: BillingClient

    // --- CONTROLES DO PAYWALL ---
    private val _paywallState = MutableStateFlow<PaywallPriceState>(PaywallPriceState.Loading)
    val paywallState: StateFlow<PaywallPriceState> = _paywallState.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    // --- ESTADOS DE FATURAMENTO (FLOWS) ---
    val isPremiumEnabled: StateFlow<Boolean> = userSettings.isPremiumFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5000),
        initialValue = false
    )

    val showSuccess: StateFlow<Boolean> = userSettings.showSuccess.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5000),
        initialValue = false
    )

    val trialStartTimestamp: StateFlow<Long> = userSettings.trialStartFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5000),
        initialValue = 0L
    )

    init {
        // 🛠️ CORREÇÃO: Inicializando a conexão com a Play Store para a query de preço
        billingClient = BillingClient.newBuilder(application)
            .setListener { _, _ -> } // Não gerencia compras aqui, só consulta preços
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("VELVET_BILLING", "✅ Conexão estabelecida com a Play Store.")
                    // Já tenta buscar o preço assim que o banco abrir
                    fetchPremiumPrice()
                } else {
                    Log.e("VELVET_BILLING", "❌ Erro ao conectar na Play Store: ${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("VELVET_BILLING", "⚠️ Conexão perdida com a Play Store.")
            }
        })

        billingHelper.startConnection()

        billingHelper.checkExistingPurchases { isPro ->
            viewModelScope.launch {
                userSettings.savePremiumStatus(isPro)
            }
        }

        viewModelScope.launch {
            repository.callLogs.collect { logs ->
                val lastLog = logs.firstOrNull()
                if (lastLog != null && System.currentTimeMillis() - lastLog.timestamp < 2000) {
                    notifyBlock(lastLog.number)
                }
            }
        }
    }

    // --- BUSCA DO PREÇO NA GOOGLE PLAY ---
    fun fetchPremiumPrice() {
        viewModelScope.launch(Dispatchers.IO) {
            _paywallState.value = PaywallPriceState.Loading

            // 🛡️ 1. ESPERA A CONEXÃO (A "REZA BRABA" EM CÓDIGO)
            // Se não estiver pronto, espera 1 segundo e tenta de novo, até 3 vezes.
            var retryCount = 0
            while (!::billingClient.isInitialized || !billingClient.isReady) {
                if (retryCount >= 3) {
                    Log.e("VELVET_BILLING", "Desistindo: Google Play não conectou após 3 tentativas.")
                    _paywallState.value = PaywallPriceState.Error
                    return@launch
                }
                Log.w("VELVET_BILLING", "Aguardando conexão com a Play Store... (Tentativa ${retryCount + 1})")
                delay(1500) // Espera 1.5s entre as tentativas
                retryCount++
            }

            // 🛡️ 2. A QUERY (SELECT) NO CONSOLE
            val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("velvet_wall_pro_lifetime") // ID mestre
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()

            billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
                val responseCode = billingResult.responseCode

                if (responseCode == BillingClient.BillingResponseCode.OK) {
                    val product = productDetailsList.firstOrNull()
                    if (product != null) {
                        val price = product.oneTimePurchaseOfferDetails?.formattedPrice ?: "R$ 29,90"

                        // Lógica de promoção baseada no valor padrão de R$ 29,90
                        val currentPriceMicros = product.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 29900000L
                        val isSale = currentPriceMicros < 29900000L

                        _paywallState.value = PaywallPriceState.Active(
                            currentPrice = price,
                            originalPrice = if (isSale) "R$ 29,90" else null,
                            discountTag = if (isSale) "OFERTA ESPECIAL" else null
                        )
                        Log.d("VELVET_BILLING", "✅ Preço carregado do Console: $price")
                    } else {
                        Log.e("VELVET_BILLING", "❌ Lista vazia. Verifique se o ID 'velvet_wall_pro_lifetime' está ATIVO no Console.")
                        _paywallState.value = PaywallPriceState.Error
                    }
                } else {
                    // LOG CRÍTICO: Se der erro, me diga qual é esse número!
                    Log.e("VELVET_BILLING", "❌ Erro Google: Código $responseCode - ${billingResult.debugMessage}")
                    _paywallState.value = PaywallPriceState.Error
                }
            }
        }
    }    // --- GESTÃO DE FATURAMENTO ---
    fun buyPremium(activity: Activity) {
        billingHelper.launchPurchaseFlow(activity)
    }

    fun restorePremium(context: Context) {
        viewModelScope.launch {
            try {
                _isRestoring.value = true
                _blockEvent.emit("Sincronizando com a Google Play...")

                launch {
                    delay(10000)
                    if (_isRestoring.value) {
                        _isRestoring.value = false
                        Log.d("VELVET", "Timeout: Google não respondeu, destravando UI.")
                    }
                }
                billingHelper.queryExistingPurchases { isPro ->
                    viewModelScope.launch {
                        try {
                            userSettings.savePremiumStatus(isPro)
                            val msg = if (isPro) "Licença PRO ativa! 🦜" else "Nenhuma licença encontrada."
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        } finally {
                            _isRestoring.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _isRestoring.value = false
                Log.e("VELVET", "Erro crítico no restauro: ${e.message}")
            }
        }
    }

    fun resetPremiumForDebug() {
        viewModelScope.launch {
            userSettings.savePremiumStatus(false)
            userSettings.setShowSuccess(false)
            Log.d("VELVET_DEBUG", "🧹 Status Premium resetado localmente.")
        }
    }

    fun triggerSuccessForDebug() {
        viewModelScope.launch {
            userSettings.setShowSuccess(true)
        }
    }

    fun triggerSuccess() {
        viewModelScope.launch {
            userSettings.setShowSuccess(true)
        }
    }

    fun dismissSuccessAnimation() {
        viewModelScope.launch {
            userSettings.setShowSuccess(false)
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

    fun moveBlackToWhite(item: BlockedNumber) {
        viewModelScope.launch {
            repository.removeFromBlacklist(item)
            repository.addToWhitelist(item.number, "Movido dos Interceptados")
        }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearLogs() }
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

    // --- TUTORIAL ---
    val showTutorial = userSettings.showTutorialFlow

    fun completeTutorial() {
        viewModelScope.launch {
            userSettings.setTutorialCompleted()
        }
    }

    val isFirstRun = userSettings.isFirstRunFlow.stateIn(
        scope = viewModelScope, // O contexto onde o Flow vai "viver"
        started = SharingStarted.WhileSubscribed(5000), // Mantém vivo por 5s após fechar a tela
        initialValue = true // 🎯 Aqui definimos o tipo explicitamente e o valor inicial
    )

    fun completeOnboarding() {
        viewModelScope.launch {
            userSettings.setFirstRunCompleted()
            // Opcional: Já iniciar o contador de 7 dias aqui se quiser ser agressivo
            userSettings.startTrialIfNecessary()
        }
    }
}

// Factory
class MainViewModelFactory(
    private val application: Application,
    private val repository: CallRepository,
    private val userSettings: UserSettings
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository, userSettings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}