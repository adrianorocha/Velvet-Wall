package blu.macaw.velvetwall.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import blu.macaw.velvetwall.R
import blu.macaw.velvetwall.data.AppDatabase
import blu.macaw.velvetwall.data.BlockedCallLog
import blu.macaw.velvetwall.data.CallRepository
import blu.macaw.velvetwall.data.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar


private const val GROUP_KEY_BLOCKS = "blu.macaw.velvetwall.BLOCK_GROUP"
private const val SUMMARY_ID = 0
private const val MAX_NOTIFICATIONS = 5
private val lastBlockTimes = mutableMapOf<String, Long>() // Mapa para silenciamento inteligente
class CallBlockerService : CallScreeningService() {

    private val repository by lazy {
        CallRepository(applicationContext, AppDatabase.getDatabase(applicationContext))
    }

    private val userSettings by lazy { UserSettings(applicationContext) }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart ?: ""
        // Normaliza para remover +55, espaços e traços
        val normalizedNumber = rawNumber.replace(Regex("[^0-9]"), "")

        Log.e("VELVET_SRV", ">>> CHAMADA: $rawNumber | Norm: $normalizedNumber <<<")

        if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {

            CoroutineScope(Dispatchers.IO).launch @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS) {
                try {
                    // --- SEGURANÇA MÁXIMA: CHECK ZERO ---
                    // Verifica se é Emergência (190, 192) ou Utilidade Pública (180, 100)
                    // Se for, liberamos IMEDIATAMENTE antes de qualquer verificação.
                    if (isEmergencyOrUtility(applicationContext, rawNumber, normalizedNumber)) {
                        Log.w(
                            "VELVET_SRV",
                            "🚨 ALERTA: Número de Emergência/Utilidade detectado! LIBERANDO: $rawNumber"
                        )
                        respondToCall(callDetails, buildResponse(false))
                        return@launch
                    }

                    // Se não for emergência, segue o fluxo normal...
                    val blockPrivate = userSettings.blockPrivateFlow.first()
                    val blockUnknown = userSettings.blockUnknownFlow.first()

                    var shouldBlock = false
                    var blockReason = ""

                    // --- 1. PRIVADO/OCULTO ---
                    val isHidden = rawNumber.isEmpty() ||
                            rawNumber.isBlank() ||
                            rawNumber.equals("private", ignoreCase = true) ||
                            rawNumber.equals("restricted", ignoreCase = true) ||
                            rawNumber.equals("unavailable", ignoreCase = true) ||
                            rawNumber.equals("unknown", ignoreCase = true)

                    if (isHidden) {
                        if (blockPrivate) {
                            shouldBlock = true
                            blockReason = "Número Privado"
                        }
                    }

                    // --- 2. LISTA NEGRA ---
                    if (!shouldBlock && repository.shouldBlockCall(normalizedNumber)) {
                        shouldBlock = true
                        blockReason = "Lista Negra"
                    }

                    // --- 3. LISTA BRANCA (Salva-vidas) ---
                    if (!shouldBlock && repository.isWhitelisted(normalizedNumber)) {
                        Log.i("VELVET_SRV", "Passou: Lista Branca.")
                        respondToCall(callDetails, buildResponse(false))
                        return@launch
                    }

                    // --- 4. DESCONHECIDOS ---
                    if (!shouldBlock && blockUnknown && !isHidden) {
                        if (hasContactPermission(applicationContext)) {
                            if (isContact(applicationContext, rawNumber)) {
                                Log.i("VELVET_SRV", "Passou: Contato salvo.")
                            } else {
                                shouldBlock = true
                                blockReason = "Desconhecido(Fora da Agenda)"
                            }
                        }
                    }

                    // --- DECISÃO FINAL ---
                    if (shouldBlock) {
                        respondToCall(callDetails, buildResponse(true))
                        showNotification(rawNumber.ifEmpty { "Privado" }, blockReason)

                        val logEntry = BlockedCallLog(
                            number = rawNumber.ifEmpty { "Privado" },
                            blockReason = blockReason,
                            timestamp = System.currentTimeMillis()
                        )
                        AppDatabase.getDatabase(applicationContext).callLogDao().log(logEntry)
                    } else {
                        respondToCall(callDetails, buildResponse(false))
                    }

                } catch (e: Exception) {
                    Log.e("VELVET_SRV", "ERRO: ${e.message}")
                    respondToCall(callDetails, buildResponse(false))
                }
            }
        }
    }

    // --- NOVA FUNÇÃO DE SEGURANÇA ---
    private fun isEmergencyOrUtility(
        context: Context,
        rawNumber: String,
        normNumber: String
    ): Boolean {
        // 1. Verifica se o Android considera emergência (Baseado no chip/país)
        // Isso cobre 190, 192, 193, 911, 112 automaticamente.
        if (PhoneNumberUtils.isEmergencyNumber(rawNumber)) {
            return true
        }

        // 2. Lista de Segurança do Brasil (Utilidade Pública)
        // Muitos desses não são "Emergência" para o Android, mas não devem ser bloqueados.
        val safeList = setOf(
            "100", // Direitos Humanos
            "180", // Central da Mulher
            "181", // Disque Denúncia
            "190", // Polícia Militar
            "191", // Polícia Rodoviária Federal
            "192", // SAMU (Ambulância)
            "193", // Bombeiros
            "194", // Polícia Federal
            "197", // Polícia Civil
            "198", // Polícia Rodoviária Estadual
            "199", // Defesa Civil
            "153", // Guarda Municipal
            "112", // Emergência Internacional (Redireciona para 190)
            "911"  // Emergência Internacional (Redireciona para 190)
        )

        // Verificamos se o número discado (limpo) termina ou é igual a um desses
        // Usamos endsWith para garantir que casos como "011190" também passem.
        return safeList.any { safeNum ->
            normNumber == safeNum || normNumber.endsWith(safeNum)
        }
    }

    // ... (Mantenha as funções hasContactPermission, isContact, buildResponse e showNotification iguais) ...
    private fun hasContactPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isContact(context: Context, number: String): Boolean {
        if (number.isBlank()) return false
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            context.contentResolver.query(uri, projection, null, null, null)?.use {
                if (it.moveToFirst()) return true
            }
        } catch (e: Exception) {
            Log.e("VELVET_SRV", "Erro contato: $e")
        }
        return false
    }

    private fun buildResponse(block: Boolean): CallResponse {
        return if (block) {
            CallResponse.Builder().setDisallowCall(true).setRejectCall(true)
                .setSkipNotification(true).setSkipCallLog(false).build()
        } else {
            CallResponse.Builder().build()
        }
    }

    // 1. Defina a ação (deve ser IGUAL à do ViewModel)
    companion object {
        const val ACTION_TEST_BLOCK = "com.blu.macaw.velvetwall.ACTION_TEST_BLOCK"
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @SuppressLint("MissingPermission")
    private fun showNotification(number: String, reason: String) {
        val channelId = "BLOCK_EVENTS_CHANNEL"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // NOME ALTERADO: currentTimeMillis para evitar conflito
        val currentTimeMillis = System.currentTimeMillis()

        // NOME ALTERADO: calendar para a lógica de horário
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // 1. Silenciamento Inteligente (Buffer de 1 minuto)
        val lastTime = lastBlockTimes[number] ?: 0L
        val isSpamming = (currentTimeMillis - lastTime) < 60000
        lastBlockTimes[number] = currentTimeMillis

        // 2. Lógica de Modo Noturno (22h às 06h)
        // Buscamos a preferência do DataStore (certifique-se de que a variável já existe no ViewModel/Settings)
        val isNightModeSettingEnabled = runBlocking { userSettings.nightModeFlow.first() }
        val applyNightSilence = isNightModeSettingEnabled && (hour >= 22 || hour < 6)

        // 3. Definição de Cores e Risco
        val isHighRisk = reason.contains("Blacklist", ignoreCase = true) || reason.contains("Spam", ignoreCase = true)
        val accentColor = if (isHighRisk) Color.parseColor("#EF4444") else ContextCompat.getColor(this, R.color.royal_cyan)

        // 4. Prioridade Dinâmica
        val priority = when {
            applyNightSilence -> NotificationCompat.PRIORITY_MIN // Totalmente silencioso à noite
            isSpamming -> NotificationCompat.PRIORITY_LOW // Baixa prioridade se for repetitivo
            else -> NotificationCompat.PRIORITY_HIGH // Normal
        }

        // 5. RemoteViews Dinâmicas
        val remoteViews = RemoteViews(packageName, R.layout.notification_block_success).apply {
            setTextViewText(R.id.notif_block_number, if (isHighRisk) "ALERTA: Número Barrado" else "Escudo: Chamada Barrada")
            setTextViewText(R.id.notif_block_reason, "$number ($reason)")

            // Cores Dinâmicas no layout arredondado
            val glow = if (isHighRisk) R.drawable.bg_icon_red_alert else R.drawable.bg_icon_cyan_glow
            setInt(R.id.icon_container, "setBackgroundResource", glow)
            setInt(R.id.notif_arrow, "setColorFilter", accentColor)
        }

        // 6. Notificação Individual
        val individualBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield_large)
            .setCustomContentView(remoteViews)
            .setGroup(GROUP_KEY_BLOCKS)
            .setPriority(priority)
            .setColor(accentColor)
            .setVibrate(if (applyNightSilence || isSpamming) longArrayOf(0) else longArrayOf(0, 100))
            .setAutoCancel(true)
            .build()

        // 7. Notificação de Resumo (ID Fixo para empilhamento)
        val summaryBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield_large)
            .setGroup(GROUP_KEY_BLOCKS)
            .setGroupSummary(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(accentColor)
            .build()

        notificationManager.notify(currentTimeMillis.toInt(), individualBuilder)
        notificationManager.notify(SUMMARY_ID, summaryBuilder)
    }
}