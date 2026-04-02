package blu.macaw.velvetwall.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
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

        // 🛡️ O GOLPE DE MESTRE: Subir o Foreground IMEDIATAMENTE (Android 14)
        // Usamos um estado inicial (ex: true) só para o Android não matar o app
        val initialNotification = buildCustomNotification(isBlockingUnknown = true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, initialNotification)
        }

        // 📊 Observa o banco e ATUALIZA a notificação em tempo real
        scope.launch {
            userSettings.blockUnknownFlow.collectLatest { isBlockingUnknown ->
                // Pega a notificação com os dados reais e atualiza silenciosamente
                val updatedNotification = buildCustomNotification(isBlockingUnknown)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(1001, updatedNotification) // Atualiza sem piscar a tela
            }
        }
        return START_STICKY
    }
    /**
     * 🏗️ Função isolada apenas para construir a notificação profissional.
     */
    @SuppressLint("RemoteViewLayout")
    private fun buildCustomNotification(isBlockingUnknown: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpenApp = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)

        val toggleIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = "TOGGLE_UNKNOWN"
            putExtra("CURRENT_STATE", isBlockingUnknown)
        }
        val pendingToggle = PendingIntent.getBroadcast(this, 1, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val settingsIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("NAVIGATE_TO", "settings")
        }
        val pendingSettings = PendingIntent.getActivity(this, 2, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val listIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("NAVIGATE_TO", "blacklist")
        }
        val pendingList = PendingIntent.getActivity(this, 3, listIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // --- DESIGN DE ELITE (RemoteViews) ---
        val customLayout = RemoteViews(packageName, R.layout.notification_control_panel)

        // Cores e Ícones Dinâmicos baseados no Padrão de Cores
        val statusText = if (isBlockingUnknown) "ESCUDO ATIVO" else "MODO MONITOR"
        val statusColor = if (isBlockingUnknown) Color.parseColor("#00E5FF") else Color.parseColor("#94A3B8")
        val toggleIcon = if (isBlockingUnknown) R.drawable.ic_power_off else R.drawable.ic_shield_large

        customLayout.setTextViewText(R.id.notif_status_text, statusText)
        customLayout.setTextColor(R.id.notif_status_text, statusColor)
// 🎯 Agora atualizamos o próprio botão diretamente!
        customLayout.setImageViewResource(R.id.btn_toggle, toggleIcon)
        customLayout.setInt(R.id.btn_toggle, "setColorFilter", statusColor)        // Atribuindo os Cliques
        customLayout.setOnClickPendingIntent(R.id.btn_toggle, pendingToggle)
        customLayout.setOnClickPendingIntent(R.id.btn_settings, pendingSettings)
        customLayout.setOnClickPendingIntent(R.id.btn_list, pendingList)

        // 🛡️ CONSTRUÇÃO DA NOTIFICAÇÃO PROFISSIONAL
        return NotificationCompat.Builder(this, "STATUS_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_shield_small_notif) // Certifique-se que este ícone existe e é branco
            .setCustomContentView(customLayout)
            .setCustomBigContentView(customLayout)
            // ❌ REMOVIDO: DecoratedCustomViewStyle (causava o corte)
            // ❌ REMOVIDO: setColorized(true) (causava o "azulão")
            .setOngoing(true)
            .setSilent(true) // Não intrusivo
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingOpenApp)
            .build()
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "STATUS_CHANNEL_ID",
                "Status do Aplicativo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Exibe o status de proteção em tempo real"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}