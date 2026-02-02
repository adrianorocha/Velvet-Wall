package blu.macaw.velvetwall.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
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

        scope.launch {
            userSettings.blockUnknownFlow.collectLatest { isBlockingUnknown ->
                updateCustomNotification(isBlockingUnknown)
            }
        }
        return START_STICKY
    }

    private fun updateCustomNotification(isBlockingUnknown: Boolean) {
        // 1. Prepara os Intents (Ações dos cliques)

        // Clicar no corpo da notificação abre o App
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpenApp = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)

        // Botão Toggle (Desativar/Ativar)
        val toggleIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = "TOGGLE_UNKNOWN"
            putExtra("CURRENT_STATE", isBlockingUnknown)
        }
        val pendingToggle = PendingIntent.getBroadcast(this, 1, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Botão Configurações
        val settingsIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "settings")
        }
        val pendingSettings = PendingIntent.getActivity(this, 2, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Botão Ver Lista
        val listIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "blacklist")
        }
        val pendingList = PendingIntent.getActivity(this, 3, listIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // 2. Configura a RemoteView (O Layout Personalizado)
        val customLayout = RemoteViews(packageName, R.layout.notification_control_panel)

        // Define os textos dinâmicos
        val statusText = if (isBlockingUnknown) "Proteção Máxima Ativa" else "Modo Monitoramento"
        val toggleText = if (isBlockingUnknown) "Desativar" else "Ativar"

        customLayout.setTextViewText(R.id.notif_status_text, statusText)
        customLayout.setTextViewText(R.id.txt_toggle, toggleText)

        // Conecta os botões aos Intents
        customLayout.setOnClickPendingIntent(R.id.btn_toggle, pendingToggle)
        customLayout.setOnClickPendingIntent(R.id.btn_settings, pendingSettings)
        customLayout.setOnClickPendingIntent(R.id.btn_list, pendingList)

        // Define ícone dinâmico se quiser (ex: escudo cinza se desativado)
        // customLayout.setImageViewResource(R.id.notif_icon, R.mipmap.ic_launcher)

        // 3. Constrói a Notificação
        val notification = NotificationCompat.Builder(this, "STATUS_CHANNEL_ID")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock) // Ícone da barra de status (obrigatório)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()) // Estilo que permite layout customizado
            .setCustomContentView(customLayout) // <--- AQUI ESTÁ A MÁGICA (Layout Colapsado)
            .setCustomBigContentView(customLayout) // <--- AQUI TAMBÉM (Layout Expandido)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)
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