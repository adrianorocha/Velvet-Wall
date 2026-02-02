package blu.macaw.velvetwall.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class CallRepository(private val context: Context, private val db: AppDatabase) {

    val blacklist: Flow<List<BlockedNumber>> = db.blacklistDao().getAll()
    val callLogs: Flow<List<BlockedCallLog>> = db.callLogDao().getAll()

    // Novo Flow da Lista Branca
    val whitelist: Flow<List<WhiteListNumber>> = db.whiteListDao().getAll()

    // --- BLACKLIST ---
    suspend fun addToBlacklist(number: String, reason: String) {
        val item = BlockedNumber(number = number, reason = reason)
        db.blacklistDao().add(item)
        // Se adicionou na Black, garante que remove da White
        db.whiteListDao().remove(WhiteListNumber(number))
    }

    suspend fun removeFromBlacklist(item: BlockedNumber) {
        db.blacklistDao().remove(item)
    }

    // --- WHITELIST ---
    suspend fun addToWhitelist(number: String, name: String) {
        val item = WhiteListNumber(number = number, name = name)
        db.whiteListDao().add(item)
        // Se adicionou na White, remove da Black
        db.blacklistDao().remove(BlockedNumber(number))
    }

    suspend fun removeFromWhitelist(item: WhiteListNumber) {
        db.whiteListDao().remove(item)
    }

    suspend fun clearLogs() {
        db.callLogDao().clear()
    }

    // --- VERIFICAÇÕES ---
    suspend fun shouldBlockCall(number: String): Boolean {
        return db.blacklistDao().isBlocked(number)
    }

    suspend fun isWhitelisted(number: String): Boolean {
        return db.whiteListDao().isWhitelisted(number)
    }

    suspend fun deleteLog(item: BlockedCallLog) = db.callLogDao().delete(item)
}
