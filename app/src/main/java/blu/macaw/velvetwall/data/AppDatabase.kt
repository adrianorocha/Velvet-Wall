package blu.macaw.velvetwall.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BlockedNumber::class, BlockedCallLog::class, WhiteListNumber::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun blacklistDao(): BlacklistDao
    abstract fun callLogDao(): CallLogDao
    abstract fun whiteListDao(): WhiteListDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // CORREÇÃO: Criamos um cadeado explícito para evitar o erro do 'this'
        private val LOCK = Any()

        fun getDatabase(context: Context): AppDatabase {
            // Usamos synchronized(LOCK) em vez de synchronized(this)
            return INSTANCE ?: synchronized(LOCK) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "velvet_wall_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}