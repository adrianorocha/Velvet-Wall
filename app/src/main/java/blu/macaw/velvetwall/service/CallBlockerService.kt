package blu.macaw.velvetwall.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import blu.macaw.velvetwall.MainActivity
import blu.macaw.velvetwall.data.AppDatabase
import blu.macaw.velvetwall.data.BlockedCallLog
import blu.macaw.velvetwall.data.CallRepository
import blu.macaw.velvetwall.data.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CallBlockerService : CallScreeningService() {

    private val repository by lazy {
        CallRepository(applicationContext, AppDatabase.getDatabase(applicationContext))
    }

    private val userSettings by lazy { UserSettings(applicationContext) }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart ?: ""
        // Normaliza removendo espaços e traços para comparação segura
        val normalizedNumber = rawNumber.replace(Regex("[^0-9+]"), "")

        Log.e("VELVET_SRV", ">>> CHAMADA RECEBIDA: $rawNumber (Norm: $normalizedNumber) <<<")

        if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {

            // Usamos runBlocking aqui para garantir que a decisão seja tomada ANTES do telefone tocar
            // (CallScreeningService precisa de resposta rápida)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val blockPrivate = userSettings.blockPrivateFlow.first()
                    val blockUnknown = userSettings.blockUnknownFlow.first()

                    Log.d("VELVET_SRV", "Configs -> Privado: $blockPrivate | Desconhecido: $blockUnknown")

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
                        } else {
                            Log.i("VELVET_SRV", "Passou: É privado mas filtro está desligado.")
                        }
                    }

                    // --- 2. LISTA NEGRA ---
                    if (!shouldBlock && repository.shouldBlockCall(normalizedNumber)) {
                        shouldBlock = true
                        blockReason = "Lista Negra"
                    }

                    // --- 3. LISTA BRANCA (A IMUNIDADE) ---
                    // Se estiver na whitelist, NADA mais importa. Ele passa.
                    if (!shouldBlock && repository.isWhitelisted(normalizedNumber)) {
                        Log.i("VELVET_SRV", "Passou: Está na Lista Branca.")
                        respondToCall(callDetails, buildResponse(false))
                        return@launch
                    }

                    // --- 4. DESCONHECIDOS (A Lógica Crítica) ---
                    // Só bloqueia se:
                    // a) O filtro está ligado
                    // b) Não é oculto (já tratado)
                    // c) Não é lista negra (já tratado)
                    if (!shouldBlock && blockUnknown && !isHidden) {

                        if (hasContactPermission(applicationContext)) {
                            // Verifica se ESTÁ na agenda
                            val isSaved = isContact(applicationContext, rawNumber)

                            if (isSaved) {
                                Log.i("VELVET_SRV", "Passou: É contato salvo.")
                            } else {
                                // NÃO É CONTATO -> BLOQUEAR!
                                shouldBlock = true
                                blockReason = "Desconhecido (Fora da Agenda)"
                                Log.e("VELVET_SRV", "BLOQUEANDO: Número $rawNumber não encontrado nos contatos.")
                            }
                        } else {
                            Log.w("VELVET_SRV", "ALERTA: Filtro de desconhecidos ativo mas SEM PERMISSÃO de contatos. Deixando passar.")
                        }
                    }

                    // --- DECISÃO FINAL ---
                    if (shouldBlock) {
                        Log.e("VELVET_SRV", ">>> BLOQUEADO! Motivo: $blockReason <<<")
                        respondToCall(callDetails, buildResponse(true))
                        showNotification(rawNumber.ifEmpty { "Privado" }, blockReason)

                        val logEntry = BlockedCallLog(
                            number = rawNumber.ifEmpty { "Privado" },
                            blockReason = blockReason
                        )
                        AppDatabase.getDatabase(applicationContext).callLogDao().log(logEntry)
                    } else {
                        respondToCall(callDetails, buildResponse(false))
                    }

                } catch (e: Exception) {
                    Log.e("VELVET_SRV", "ERRO FATAL: ${e.message}")
                    e.printStackTrace()
                    respondToCall(callDetails, buildResponse(false))
                }
            }
        }
    }

    // --- FUNÇÕES AUXILIARES ---

    private fun hasContactPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isContact(context: Context, number: String): Boolean {
        if (number.isBlank()) return false

        try {
            // Tenta buscar pelo número exato e normalizado
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.DISPLAY_NAME)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                    Log.d("VELVET_SRV", "Contato encontrado: $name ($number)")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("VELVET_SRV", "Erro ao buscar contato: $e")
        }

        Log.d("VELVET_SRV", "Número NÃO é contato: $number")
        return false
    }

    private fun buildResponse(block: Boolean): CallResponse {
        return if (block) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(true)
                .setSkipCallLog(false)
                .build()
        } else {
            CallResponse.Builder().build()
        }
    }

    private fun showNotification(number: String, reason: String) {
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

        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) { Log.e("VELVET_SRV", "Erro notif: $e") }
    }
}