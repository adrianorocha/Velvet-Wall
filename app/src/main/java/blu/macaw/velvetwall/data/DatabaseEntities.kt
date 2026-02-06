package blu.macaw.velvetwall.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- TABELAS ---

@Entity(tableName = "blacklist")
data class BlockedNumber(
    @PrimaryKey val number: String,
    val reason: String = "Manual",
    val addedAt: Long = System.currentTimeMillis()
)

// NOVA TABELA: LISTA BRANCA
@Entity(tableName = "whitelist")
data class WhiteListNumber(
    @PrimaryKey val number: String,
    val name: String = "Autorizado",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_logs")
data class BlockedCallLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val timestamp: Long = System.currentTimeMillis(),
    val blockReason: String
)

// --- DAOS ---

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklist ORDER BY addedAt DESC")
    fun getAll(): Flow<List<BlockedNumber>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(number: BlockedNumber): Long

    @Delete
    suspend fun remove(number: BlockedNumber): Int

    @Query("SELECT EXISTS(SELECT 1 FROM blacklist WHERE number = :number)")
    suspend fun isBlocked(number: String): Boolean
}

// NOVO DAO
@Dao
interface WhiteListDao {
    @Query("SELECT * FROM whitelist ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WhiteListNumber>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(number: WhiteListNumber): Long

    @Delete
    suspend fun remove(number: WhiteListNumber): Int

    @Query("SELECT EXISTS(SELECT 1 FROM whitelist WHERE number = :number)")
    suspend fun isWhitelisted(number: String): Boolean
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BlockedCallLog>>

    @Insert
    suspend fun log(entry: BlockedCallLog): Long

    // --- NOVO: Para apagar um item específico ---
    @Delete
    suspend fun delete(entry: BlockedCallLog): Int

    @Query("DELETE FROM call_logs")
    suspend fun clear(): Int

    @Query("DELETE FROM call_logs WHERE timestamp < :threshold")
    suspend fun deleteOldLogs(threshold: Long)
}
