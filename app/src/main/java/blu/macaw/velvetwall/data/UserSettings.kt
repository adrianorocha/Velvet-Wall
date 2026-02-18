package blu.macaw.velvetwall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Cria o arquivo físico de salvamento
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserSettings(private val context: Context) {

    // Definindo as chaves (os nomes das variáveis no arquivo)
    companion object {
        val BLOCK_PRIVATE = booleanPreferencesKey("block_private")
        val BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val CLEANUP_DAYS = intPreferencesKey("cleanup_days")
    }

    // --- LEITURA (Flows que atualizam a tela automaticamente) ---

    val blockPrivateFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[BLOCK_PRIVATE] ?: true } // Padrão: Ativado

    val blockUnknownFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[BLOCK_UNKNOWN] ?: false } // Padrão: Desativado

    val notificationsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFICATIONS_ENABLED] ?: true } // Padrão: Ativado

    val biometricFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[BIOMETRIC_ENABLED] ?: true } // Padrão: Ativado


    val cleanupDaysFlow: Flow<Int> = context.dataStore.data.map { it[CLEANUP_DAYS] ?: 30 }

    suspend fun setCleanupDays(days: Int) {
        context.dataStore.edit { it[CLEANUP_DAYS] = days }
    }
    // --- ESCRITA (Funções para salvar) ---

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

    private val NIGHT_MODE_KEY = booleanPreferencesKey("night_mode_enabled")

    val nightModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NIGHT_MODE_KEY] ?: true // Ativado por padrão
        }

    suspend fun setNightMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NIGHT_MODE_KEY] = enabled
        }
    }
}