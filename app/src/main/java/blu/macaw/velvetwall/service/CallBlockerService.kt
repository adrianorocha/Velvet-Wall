package blu.macaw.velvetwall.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import blu.macaw.velvetwall.MainActivity
import blu.macaw.velvetwall.R
import blu.macaw.velvetwall.data.AppDatabase
import blu.macaw.velvetwall.data.BlockedCallLog
import blu.macaw.velvetwall.data.CallRepository
import blu.macaw.velvetwall.data.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


private const val GROUP_KEY_BLOCKS = "blu.macaw.velvetwall.BLOCK_GROUP"
private const val SUMMARY_ID = 0
private const val MAX_NOTIFICATIONS = 5
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

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @SuppressLint("MissingPermission")
    private fun showNotification(number: String, reason: String) {
        val channelId = "BLOCK_EVENTS_CHANNEL"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Limpeza de excedentes (Manter apenas as 5 mais recentes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNotifs = notificationManager.activeNotifications
                .filter { it.notification.group == GROUP_KEY_BLOCKS && it.id != SUMMARY_ID }
                .sortedBy { it.postTime }

            if (activeNotifs.size >= MAX_NOTIFICATIONS) {
                notificationManager.cancel(activeNotifs.first().id)
            }
        }

        // 2. RemoteViews (Layout Customizado)
        val remoteViews = RemoteViews(packageName, R.layout.notification_block_success).apply {
            setTextViewText(R.id.notif_block_number, "Barrado: $number")
            setTextViewText(R.id.notif_block_reason, reason)
        }

        // 3. Notificação Individual
        val individualBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield_large)
            .setCustomContentView(remoteViews)
            // .setStyle(...)  <-- REMOVA ESTA LINHA se quiser o visual 100% customizado e arredondado
            .setGroup(GROUP_KEY_BLOCKS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColorized(true)
            .setColor(ContextCompat.getColor(this, R.color.royal_cyan))
            .setAutoCancel(true)
            .build()

        // 4. Notificação de Resumo (Obrigatória para não "se perder")
        val summaryBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield_large)
            .setGroup(GROUP_KEY_BLOCKS)
            .setGroupSummary(true) // Crucial para o empilhamento
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Postar a individual com ID único e o Resumo com ID fixo
        notificationManager.notify(System.currentTimeMillis().toInt(), individualBuilder)
        notificationManager.notify(SUMMARY_ID, summaryBuilder)
    }}