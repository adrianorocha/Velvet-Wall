package blu.macaw.velvetwall.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import blu.macaw.velvetwall.R

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "BLOCK_EVENTS_CHANNEL"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bloqueios do Velvet Wall",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de chamadas interceptadas"
                enableLights(true)
                lightColor = Color.CYAN
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(number: String, reason: String, isTest: Boolean = false) {
        val currentTime = System.currentTimeMillis()

        // Configuração das RemoteViews (Seu layout premium)
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_block_success).apply {
            setTextViewText(R.id.notif_block_number, if (isTest) "🛡️ Teste de Proteção" else "Chamada Barrada")
            setTextViewText(R.id.notif_block_reason, "$number ($reason)")

            // Aplicando o Royal Cyan da Blu Macaw
            val accentColor = Color.parseColor("#22D3EE")
            setInt(R.id.icon_container, "setBackgroundResource", R.drawable.bg_icon_cyan_glow)
            setInt(R.id.notif_arrow, "setColorFilter", accentColor)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield_large)
            .setCustomContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 100))
            .setAutoCancel(true)

        // Usamos IDs diferentes para não sobrepor se o usuário clicar várias vezes no teste
        notificationManager.notify(currentTime.toInt(), builder.build())
    }
}