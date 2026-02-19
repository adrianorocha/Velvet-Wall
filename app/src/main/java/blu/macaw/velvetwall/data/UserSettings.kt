package blu.macaw.velvetwall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Cria a instância única do DataStore para o app
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserSettings(private val context: Context) {

    // Definindo as chaves
    companion object {
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

    // --- LEITURA (Flows que atualizam a tela e o serviço automaticamente) ---
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
}