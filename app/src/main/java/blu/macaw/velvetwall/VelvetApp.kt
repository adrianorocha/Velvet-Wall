package blu.macaw.velvetwall

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class VelvetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "STATUS_CHANNEL_ID",
                "Status do Velvet Wall",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Mantém o app ativo em segundo plano"

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}