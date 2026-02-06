package blu.macaw.velvetwall.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import blu.macaw.velvetwall.data.AppDatabase
import java.util.concurrent.TimeUnit

class CleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getDatabase(applicationContext).callLogDao()

        // Recupera o valor enviado pela UI. Se falhar, usa 30 como fallback.
        val daysToKeep = inputData.getInt("DAYS_TO_KEEP", 30)

        // Calcula o timestamp de corte (Threshold)
        val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysToKeep.toLong())

        return try {
            dao.deleteOldLogs(threshold) // Deleta tudo que for menor (mais antigo) que o threshold
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}