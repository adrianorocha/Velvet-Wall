package blu.macaw.velvetwall.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo // <--- NOVO IMPORT OBRIGATÓRIO
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import blu.macaw.velvetwall.MainActivity
import blu.macaw.velvetwall.R
import blu.macaw.velvetwall.data.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppStatusService : Service() {

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val userSettings by lazy { UserSettings(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        scope.launch {
            userSettings.blockUnknownFlow.collectLatest { isBlockingUnknown ->
                updateCustomNotification(isBlockingUnknown)
            }
        }
        return START_STICKY
    }

    private fun updateCustomNotification(isBlockingUnknown: Boolean) {
        // 1. Prepara os Intents (Ações dos cliques) - MANTIDO INTACTO
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpenApp = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)

        val toggleIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = "TOGGLE_UNKNOWN"
            putExtra("CURRENT_STATE", isBlockingUnknown)
        }
        val pendingToggle = PendingIntent.getBroadcast(this, 1, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val settingsIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "settings")
        }
        val pendingSettings = PendingIntent.getActivity(this, 2, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val listIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "blacklist")
        }
        val pendingList = PendingIntent.getActivity(this, 3, listIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // 2. Configura a RemoteView (O Layout Personalizado) - MANTIDO INTACTO
        val customLayout = RemoteViews(packageName, R.layout.notification_control_panel)

        val statusText = if (isBlockingUnknown) "Proteção Máxima Ativa" else "Modo Monitoramento"
        val toggleText = if (isBlockingUnknown) "Desativar" else "Ativar"
        val toggleIcon = if (isBlockingUnknown) R.drawable.ic_power_off else R.drawable.ic_shield_large

        customLayout.setTextViewText(R.id.notif_status_text, statusText)
        customLayout.setTextViewText(R.id.txt_toggle, toggleText)
        customLayout.setImageViewResource(R.id.icon_toggle, toggleIcon)

        customLayout.setOnClickPendingIntent(R.id.btn_toggle, pendingToggle)
        customLayout.setOnClickPendingIntent(R.id.btn_settings, pendingSettings)
        customLayout.setOnClickPendingIntent(R.id.btn_list, pendingList)

        // 3. Constrói a Notificação - MANTIDO INTACTO
        val royalCyanColor = ContextCompat.getColor(this, R.color.royal_cyan)

        val notification = NotificationCompat.Builder(this, "STATUS_CHANNEL_ID")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setColor(royalCyanColor)
            .setColorized(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customLayout)
            .setCustomBigContentView(customLayout)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // 4. A MARRETADA DO ANDROID 14 (O QUE MUDOU AQUI)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1001,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE // Passaporte para rodar no background
            )
        } else {
            // Comportamento padrão para Android 13 ou inferior
            startForeground(1001, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "STATUS_CHANNEL_ID",
                "Status do Aplicativo",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.enableLights(false)
            channel.enableVibration(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}