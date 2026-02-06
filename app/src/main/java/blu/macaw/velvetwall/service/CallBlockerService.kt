package blu.macaw.velvetwall.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
                        Log.w("VELVET_SRV", "🚨 ALERTA: Número de Emergência/Utilidade detectado! LIBERANDO: $rawNumber")
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
    private fun isEmergencyOrUtility(context: Context, rawNumber: String, normNumber: String): Boolean {
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
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    private fun isContact(context: Context, number: String): Boolean {
        if (number.isBlank()) return false
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            context.contentResolver.query(uri, projection, null, null, null)?.use {
                if (it.moveToFirst()) return true
            }
        } catch (e: Exception) { Log.e("VELVET_SRV", "Erro contato: $e") }
        return false
    }

    private fun buildResponse(block: Boolean): CallResponse {
        return if (block) {
            CallResponse.Builder().setDisallowCall(true).setRejectCall(true).setSkipNotification(true).setSkipCallLog(false).build()
        } else {
            CallResponse.Builder().build()
        }
    }

/*    private fun showNotification(number: String, reason: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, "CHANNEL_BLOCK_ID")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Velvet Wall: $reason")
            .setContentText("Bloqueou: $number")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        try { NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build()) } catch (e: Exception) {}
    }*/

// Dentro do seu CallBlockerService.kt

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(number: String, reason: String) {
//    private fun showBlockSuccessAnimation(number: String, reason: String) {
        val channelId = "BLOCK_EVENTS_CHANNEL"

        // Intent para abrir o histórico ao clicar
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("NAVIGATE_TO", "history")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Construção da Notificação com Estilo "Big Picture" ou Ícone de Escudo
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield_large) // Seu logo Blu Macaw
            .setContentTitle("Escudo Ativado: Chamada Barrada")
            .setContentText("O número $number foi bloqueado ($reason).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(ContextCompat.getColor(this, R.color.royal_cyan))
            .setColorized(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // Adiciona um efeito sonoro discreto e vibração curta (Premium feel)
            .setVibrate(longArrayOf(0, 100, 50, 100))
            .setPublicVersion(
                NotificationCompat.Builder(this, channelId)
                    .setContentTitle("Velvet Wall")
                    .setContentText("Uma chamada indesejada foi bloqueada.")
                    .build()
            )

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
    }
}