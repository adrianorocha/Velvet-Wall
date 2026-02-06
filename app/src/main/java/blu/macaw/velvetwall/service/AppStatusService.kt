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
        // 1. Prepara os Intents (Ações dos cliques) - IGUAL AO ANTERIOR
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

        // 2. Configura a RemoteView (O Layout Personalizado)
        val customLayout = RemoteViews(packageName, R.layout.notification_control_panel)

        // Define os textos e ícones dinâmicos
        val statusText = if (isBlockingUnknown) "Proteção Máxima Ativa" else "Modo Monitoramento"
        val toggleText = if (isBlockingUnknown) "Desativar" else "Ativar"
        // Ícone de "tomada" ou "escudo" que muda conforme o estado
        val toggleIcon = if (isBlockingUnknown) R.drawable.ic_power_off else R.drawable.ic_shield_large // Use um ícone de 'ligar' se tiver

        customLayout.setTextViewText(R.id.notif_status_text, statusText)
        customLayout.setTextViewText(R.id.txt_toggle, toggleText)
        // Atualiza o ícone do botão de alternar
        customLayout.setImageViewResource(R.id.icon_toggle, toggleIcon)

        // Conecta os botões aos Intents
        customLayout.setOnClickPendingIntent(R.id.btn_toggle, pendingToggle)
        customLayout.setOnClickPendingIntent(R.id.btn_settings, pendingSettings)
        customLayout.setOnClickPendingIntent(R.id.btn_list, pendingList)

        // 3. Constrói a Notificação
        // Pegando a cor Ciano dos recursos para usar no builder
        val royalCyanColor = ContextCompat.getColor(this, R.color.royal_cyan)

        val notification = NotificationCompat.Builder(this, "STATUS_CHANNEL_ID")
            // Ícone pequeno obrigatório para a barra de status (deve ser branco/transparente)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            // Define a cor de destaque da notificação (usada pelo sistema)
            .setColor(royalCyanColor)
            // Habilita a notificação colorida (essencial para o fundo escuro funcionar bem)
            .setColorized(true)
            // Define o estilo de layout personalizado
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customLayout)
            .setCustomBigContentView(customLayout)
            // Define que é uma notificação de serviço em andamento
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
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