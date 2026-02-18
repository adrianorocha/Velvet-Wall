package blu.macaw.velvetwall.ui

import android.app.Application
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import blu.macaw.velvetwall.data.BlockedCallLog
import blu.macaw.velvetwall.data.BlockedNumber
import blu.macaw.velvetwall.data.CallRepository
import blu.macaw.velvetwall.data.UserSettings
import blu.macaw.velvetwall.data.WhiteListNumber
import blu.macaw.velvetwall.data.worker.CleanupWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.work.Constraints
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    application: Application,
    private val repository: CallRepository,
    private val userSettings: UserSettings
) : AndroidViewModel(application) {

    init {
        // Observa o histórico. Sempre que mudar, pegamos o último e notificamos a UI
        viewModelScope.launch {
            repository.callLogs.collect { logs ->
                val lastLog = logs.firstOrNull()
                // Se o log for recente (últimos 2 segundos), avisamos o escudo
                if (lastLog != null && System.currentTimeMillis() - lastLog.timestamp < 2000) {
                    notifyBlock(lastLog.number)
                }
            }
        }
    }

    // Lists
    val blacklist = repository.blacklist
    val whitelist = repository.whitelist // <--- Nova lista
    val history = repository.callLogs

    // Settings
    val blockPrivate = userSettings.blockPrivateFlow
    val blockUnknown = userSettings.blockUnknownFlow
    val notificationsEnabled = userSettings.notificationsFlow
    val biometricEnabled = userSettings.biometricFlow

    private val context = getApplication<Application>()

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    val cleanupDays = userSettings.cleanupDaysFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 30
    )
    fun updateCleanupSettings(days: Int) {
        viewModelScope.launch {
            // 1. Grava no DataStore (Persistência real)
            userSettings.setCleanupDays(days)

            // 2. Agenda a tarefa no Android
            scheduleCleanup(days)
        }
    }
    private val _blockEvent = MutableSharedFlow<String>(
        replay = 0, // Não queremos que mensagens antigas apareçam ao trocar de tela
        extraBufferCapacity = 1
    )
    val blockEvent = _blockEvent.asSharedFlow() // Use StateFlow para melhor integração com UI

    fun notifyBlock(number: String) {
        viewModelScope.launch {
            _blockEvent.emit("🛡️ Escudo Ativado: $number bloqueado!")

            // Pequeno delay para "resetar" o fluxo e permitir novos pop-ups
            delay(3000)
            _blockEvent.emit("")
        }
    }
    // --- AÇÕES BLACKLIST ---
    fun addToBlacklist(number: String, reason: String = "Manual") {
        viewModelScope.launch { repository.addToBlacklist(number, reason) }
    }

    fun removeFromBlacklist(item: BlockedNumber) {
        viewModelScope.launch { repository.removeFromBlacklist(item) }
    }

    // --- AÇÕES WHITELIST ---
    fun addToWhitelist(number: String, name: String = "Autorizado") {
        viewModelScope.launch { repository.addToWhitelist(number, name) }
    }

    fun removeFromWhitelist(item: WhiteListNumber) {
        viewModelScope.launch { repository.removeFromWhitelist(item) }
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

    fun deleteLog(item: BlockedCallLog) {
        viewModelScope.launch { repository.deleteLog(item) } // Precisa criar no Repository também*
    }

    // Ação: "Ops, esse número é amigo!"
    fun allowFromHistory(item: BlockedCallLog) {
        viewModelScope.launch {
            // 1. Adiciona na Lista Branca
            repository.addToWhitelist(item.number, "Desbloqueado via Histórico")
            // 2. Opcional: Remove o log de "bloqueado" pois foi um erro
            repository.deleteLog(item)
        }
    }


    val nightModeEnabled = userSettings.nightModeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true // Valor inicial
    )

    fun toggleNightMode(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setNightMode(enabled)
        }
    }
    // Ação: "Isso, continue bloqueando!"
    fun blockFromHistory(item: BlockedCallLog) {
        viewModelScope.launch {
            // Adiciona na Lista Negra explicitamente
            repository.addToBlacklist(item.number, "Confirmado via Histórico")
        }
    }

    fun setBlockPrivate(enabled: Boolean) = viewModelScope.launch { userSettings.setBlockPrivate(enabled) }
    fun setBlockUnknown(enabled: Boolean) = viewModelScope.launch { userSettings.setBlockUnknown(enabled) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { userSettings.setNotifications(enabled) }
    fun setBiometric(enabled: Boolean) = viewModelScope.launch { userSettings.setBiometric(enabled) }

    fun checkRoleStatus(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            val isHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            _isServiceActive.value = isHeld
        } else {
            _isServiceActive.value = true
        }
    }

    fun scheduleCleanup(days: Int) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true) // Só limpa se tiver bateria
            .setRequiresDeviceIdle(true)   // Só limpa quando o usuário não estiver usando
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("log_cleanup")
            .build()

        WorkManager.getInstance(getApplication())
            .enqueueUniquePeriodicWork(
                "VelvetCleanup",
                ExistingPeriodicWorkPolicy.REPLACE, // Atualiza se mudar os dias
                cleanupRequest
            )
    }

    fun clearEverything() {
        viewModelScope.launch {
            // 1. Limpa o Banco de Dados
            repository.clearLogs()

            // 2. Limpa a Central de Notificações
            val notificationManager = getApplication<Application>()
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Cancela as individuais e o resumo (Summary)
            notificationManager.cancelAll()
        }
    }

    fun triggerTestNotification() {
        viewModelScope.launch {
            try {
                // Simulamos os dados de uma chamada bloqueada para o teste
                val testNumber = "+55 (11) 99999-9999"
                val testReason = "Teste de Proteção Velvet Wall"

                // Aqui chamamos a função que você já refatorou no Service
                // Certifique-se de que o ViewModel tem acesso à instância do serviço ou via Intent
                sendTestBroadcast(testNumber, testReason)
            } catch (e: Exception) {
                Log.e("VelvetWall", "Erro ao disparar teste: ${e.message}")
            }
        }
    }

    // Função auxiliar para disparar o teste via Broadcast (caso o serviço seja isolado)
    private fun sendTestBroadcast(number: String, reason: String) {
        val intent = Intent("blu.macaw.velvetwall.ACTION_TEST_BLOCK").apply {
            // Especificamos o pacote para maior segurança (Explicit Intent)
            setPackage(context.packageName)
            putExtra("number", number)
            putExtra("reason", reason)
        }
        // Agora o context.sendBroadcast funcionará perfeitamente!
        context.sendBroadcast(intent)
    }

}

// Factory (igual)
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



