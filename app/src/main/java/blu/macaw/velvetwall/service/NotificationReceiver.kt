package blu.macaw.velvetwall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import blu.macaw.velvetwall.data.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val settings = UserSettings(context)
        val scope = CoroutineScope(Dispatchers.IO)

        when (intent.action) {
            "TOGGLE_UNKNOWN" -> {
                // Ação: Ligar/Desligar bloqueio de desconhecidos
                scope.launch {
                    val isCurrentlyEnabled = intent.getBooleanExtra("CURRENT_STATE", false)
                    settings.setBlockUnknown(!isCurrentlyEnabled)

                    // Atualiza a notificação (reinicia o serviço para redesenhar)
                    val serviceIntent = Intent(context, AppStatusService::class.java)
                    context.startForegroundService(serviceIntent)
                }
            }
            "OPEN_BLACKLIST" -> {
                // Ação: Abrir direto na tela de Lista
                val appIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                appIntent?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("NAVIGATE_TO", "blacklist") // Vamos tratar isso na MainActivity
                }
                context.startActivity(appIntent)
            }
        }
    }
}