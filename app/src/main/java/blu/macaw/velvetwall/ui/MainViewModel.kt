package blu.macaw.velvetwall.ui

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import blu.macaw.velvetwall.data.BlockedCallLog
import blu.macaw.velvetwall.data.BlockedNumber
import blu.macaw.velvetwall.data.CallRepository
import blu.macaw.velvetwall.data.UserSettings
import blu.macaw.velvetwall.data.WhiteListNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: CallRepository,
    private val userSettings: UserSettings
) : ViewModel() {

    // Lists
    val blacklist = repository.blacklist
    val whitelist = repository.whitelist // <--- Nova lista
    val history = repository.callLogs

    // Settings
    val blockPrivate = userSettings.blockPrivateFlow
    val blockUnknown = userSettings.blockUnknownFlow
    val notificationsEnabled = userSettings.notificationsFlow
    val biometricEnabled = userSettings.biometricFlow

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

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
}

// Factory (igual)
class MainViewModelFactory(
    private val repository: CallRepository,
    private val userSettings: UserSettings
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, userSettings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

