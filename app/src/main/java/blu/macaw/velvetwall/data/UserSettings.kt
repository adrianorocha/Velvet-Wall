package blu.macaw.velvetwall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


// Cria a instância única do DataStore para o app
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserSettings(private val context: Context) {

    // Definindo as chaves
    companion object {
        val TRIAL_START_TIMESTAMP = longPreferencesKey("trial_start_timestamp")
        val SHOW_SUCCESS = booleanPreferencesKey("show_success")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val BLOCK_PRIVATE = booleanPreferencesKey("block_private")
        val BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val CLEANUP_DAYS = intPreferencesKey("cleanup_days")
        val NIGHT_MODE_KEY = booleanPreferencesKey("night_mode_enabled")
        val BLOCKED_DDDS_KEY = stringSetPreferencesKey("blocked_ddds")
        val PARANOID_MODE_KEY = booleanPreferencesKey("paranoid_mode")
        val USER_LOCAL_DDD_KEY = stringPreferencesKey("user_local_ddd")
        val STEALTH_MODE_KEY = booleanPreferencesKey("stealth_mode")
    }

    private val SHOW_TUTORIAL_KEY = booleanPreferencesKey("show_tutorial")


    // --- LEITURA (Flows que atualizam a tela e o serviço automaticamente) ---
    // Fluxos de leitura
    val isPremiumFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_PREMIUM] ?: false }

    val trialStartFlow: Flow<Long> = context.dataStore.data.map { it[TRIAL_START_TIMESTAMP] ?: 0L }
    val blockPrivateFlow: Flow<Boolean> = context.dataStore.data.map { it[BLOCK_PRIVATE] ?: true }
    val blockUnknownFlow: Flow<Boolean> = context.dataStore.data.map { it[BLOCK_UNKNOWN] ?: false }
    val notificationsFlow: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val biometricFlow: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED] ?: true }
    val cleanupDaysFlow: Flow<Int> = context.dataStore.data.map { it[CLEANUP_DAYS] ?: 30 }
    val nightModeFlow: Flow<Boolean> = context.dataStore.data.map { it[NIGHT_MODE_KEY] ?: true }
    val blockedDDDsFlow: Flow<Set<String>> = context.dataStore.data.map { it[BLOCKED_DDDS_KEY] ?: emptySet() }
    val paranoidModeFlow: Flow<Boolean> = context.dataStore.data.map { it[PARANOID_MODE_KEY] ?: false }
    val userLocalDDDFlow: Flow<String> = context.dataStore.data.map { it[USER_LOCAL_DDD_KEY] ?: "" }
    val stealthModeFlow: Flow<Boolean> = context.dataStore.data.map { it[STEALTH_MODE_KEY] ?: false }

    // --- ESCRITA (Funções para salvar no disco) ---
    suspend fun setBlockPrivate(enabled: Boolean) {
        context.dataStore.edit { it[BLOCK_PRIVATE] = enabled }
    }

    suspend fun setBlockUnknown(enabled: Boolean) {
        context.dataStore.edit { it[BLOCK_UNKNOWN] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setBiometric(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setCleanupDays(days: Int) {
        context.dataStore.edit { it[CLEANUP_DAYS] = days }
    }

    suspend fun setNightMode(enabled: Boolean) {
        context.dataStore.edit { it[NIGHT_MODE_KEY] = enabled }
    }

    suspend fun setStealthMode(enabled: Boolean) {
        context.dataStore.edit { it[STEALTH_MODE_KEY] = enabled }
    }

    suspend fun setParanoidMode(enabled: Boolean) {
        context.dataStore.edit { it[PARANOID_MODE_KEY] = enabled }
    }

    suspend fun saveUserLocalDDD(ddd: String) {
        context.dataStore.edit { it[USER_LOCAL_DDD_KEY] = ddd }
    }

    suspend fun saveDDD(ddd: String) {
        context.dataStore.edit { preferences ->
            val currentSet = preferences[BLOCKED_DDDS_KEY] ?: emptySet()
            preferences[BLOCKED_DDDS_KEY] = currentSet + ddd
        }
    }

    suspend fun removeDDD(ddd: String) {
        context.dataStore.edit { preferences ->
            val currentSet = preferences[BLOCKED_DDDS_KEY] ?: emptySet()
            preferences[BLOCKED_DDDS_KEY] = currentSet - ddd
        }
    }

    // Funções de escrita
    suspend fun setPremium(enabled: Boolean) = context.dataStore.edit { it[IS_PREMIUM] = enabled }

    suspend fun startTrialIfNecessary() {
        context.dataStore.edit { pref ->
            if ((pref[TRIAL_START_TIMESTAMP] ?: 0L) == 0L) {
                pref[TRIAL_START_TIMESTAMP] = System.currentTimeMillis()
            }
        }
    }

    suspend fun isFeatureAllowed(): Boolean {
        val isPremium = isPremiumFlow.first()
        if (isPremium) return true // Usuário PRO tem acesso total

        val trialStart = trialStartFlow.first()
        if (trialStart == 0L) return true // Trial ainda não começou, liberado por enquanto

        val sevenDaysInMillis = 14 * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        // Retorna TRUE se ainda estiver dentro dos 14 dias de degustação
        return (currentTime - trialStart) < sevenDaysInMillis
    }

    // Dentro da classe UserSettings
    suspend fun setShowSuccess(show: Boolean) {
        context.dataStore.edit { it[SHOW_SUCCESS] = show }
    }

    val showSuccess: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SHOW_SUCCESS] ?: false }

    suspend fun savePremiumStatus(isPro: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_PREMIUM] = isPro
        }
    }

    // No seu arquivo de preferências (DataStore)
    // Flow para observar se devemos mostrar o tutorial
    val showTutorialFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_TUTORIAL_KEY] ?: true }

    // Função para marcar como concluído
    suspend fun setTutorialCompleted() {
        context.dataStore.edit { it[SHOW_TUTORIAL_KEY] = false }
    }

    // 4. A Gravação (O seu código de disable)
    suspend fun disableTutorial() {
        context.dataStore.edit { it[SHOW_TUTORIAL_KEY] = false }
    }
}