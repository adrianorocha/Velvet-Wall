package blu.macaw.velvetwall.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.PhoneNumberUtils.isEmergencyNumber
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import blu.macaw.velvetwall.R
import blu.macaw.velvetwall.data.AppDatabase
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
private val lastBlockTimes = mutableMapOf<String, Long>()

@Suppress("DEPRECATION")
class CallBlockerService : CallScreeningService() {

    private val repository by lazy {
        CallRepository(applicationContext, AppDatabase.getDatabase(applicationContext))
    }

    private val userSettings by lazy { UserSettings(applicationContext) }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        val rawNumber = callDetails.handle?.schemeSpecificPart ?: ""
        val normalizedNumber = rawNumber.replace(Regex("[^0-9]"), "")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Carregar preferências e Status de Faturamento
                val isPremium = userSettings.isPremiumFlow.first()
                val isFeatureAllowed = userSettings.isFeatureAllowed() // Verifica Trial ou Premium

                val isParanoidActive = userSettings.paranoidModeFlow.first()
                val myDDD = userSettings.userLocalDDDFlow.first()
                val blockPrivate = userSettings.blockPrivateFlow.first()
                val blockUnknown = userSettings.blockUnknownFlow.first()
                val blockedDDDs = userSettings.blockedDDDsFlow.first()

                var shouldBlock = false
                var blockReason = ""
                var isTrialFeature = false // Marca se o bloqueio depende da degustação

                // 2. Identificação básica
                val isHidden = rawNumber.isEmpty() || rawNumber.isBlank() || rawNumber.equals("private", ignoreCase = true)
                val incomingDDD = extractDDD(normalizedNumber)
                val isContact = if (!isHidden) isContact(applicationContext, rawNumber) else false

                // 3. Hierarquia de Bloqueio com Lógica de Vendas
                when {
                    isEmergencyOrUtility(applicationContext, rawNumber, normalizedNumber) ||
                            repository.isWhitelisted(normalizedNumber) -> {
                        respondToCall(callDetails, CallResponse.Builder().build())
                        return@launch
                    }

                    // Lista Negra (Recurso Free)
                    repository.shouldBlockCall(normalizedNumber) -> {
                        shouldBlock = true; blockReason = "Interceptados"
                    }

                    // DDD Bloqueado (Recurso de Degustação)
                    incomingDDD != null && blockedDDDs.contains(incomingDDD) -> {
                        shouldBlock = true; blockReason = "DDD $incomingDDD Bloqueado"; isTrialFeature = true
                    }

                    // Modo Paranóico (Recurso de Degustação)
                    isParanoidActive && myDDD.isNotEmpty() && !isContact && incomingDDD != myDDD -> {
                        shouldBlock = true; blockReason = "Modo Paranóico"; isTrialFeature = true
                    }

                    // Desconhecidos (Recurso de Degustação)
                    blockUnknown && !isHidden && !isContact -> {
                        shouldBlock = true; blockReason = "Fora da Agenda"; isTrialFeature = true
                    }

                    // Número Privado (Recurso Free)
                    isHidden && blockPrivate -> {
                        shouldBlock = true; blockReason = "Número Privado"
                    }
                }

                // 4. Execução Final com Filtro de Monetização
                if (shouldBlock) {
                    if (isTrialFeature && !isFeatureAllowed) {
                        // Trial expirou: Avisa o usuário mas NÃO bloqueia a chamada (Gatilho de Perda)
                        showUpsellNotification(rawNumber.ifEmpty { "Privado" }, blockReason)
                        respondToCall(callDetails, CallResponse.Builder().build())
                    } else {
                        // Se for a primeira vez usando Trial, iniciamos o contador agora
                        if (isTrialFeature && !isPremium) userSettings.startTrialIfNecessary()

                        blockAndNotify(callDetails, rawNumber.ifEmpty { "Privado" }, blockReason)
                        repository.logBlockedCall(rawNumber.ifEmpty { "Privado" }, blockReason)
                    }
                } else {
                    respondToCall(callDetails, CallResponse.Builder().build())
                }
            } catch (e: Exception) {
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }

    private fun blockAndNotify(callDetails: Call.Details, number: String, reason: String) {
        val isPremium = runBlocking { userSettings.isPremiumFlow.first() }
        val isStealthActive = runBlocking { userSettings.stealthModeFlow.first() }
        val isNotifEnabled = runBlocking { userSettings.notificationsFlow.first() }

        // O Modo Stealth é exclusivo para quem é PREMIUM
        val actuallyStealth = isStealthActive && isPremium

        if (actuallyStealth) {
            triggerHapticFeedback()
        }

        // Se o usuário ativou Stealth mas não é Premium, ele recebe a notificação normal
        // para ser lembrado de que precisa do PRO para a invisibilidade total.
        if (!actuallyStealth && isNotifEnabled) {
            showNotification(number, reason)
        }

        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(actuallyStealth)
            .build()

        respondToCall(callDetails, response)
    }

    private fun showUpsellNotification(number: String, reason: String) {
        // Notificação especial para converter o usuário
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "BLOCK_EVENTS_CHANNEL"

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield_large)
            .setContentTitle("Spam Detectado: $number")
            .setContentText("O Trial do $reason expirou. Ative o PRO para barrar!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(Color.parseColor("#FACC15")) // Amarelo para atenção
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), builder)
    }

    // --- MANTIDAS AS FUNÇÕES AUXILIARES SEM ALTERAÇÃO ---
    @SuppressLint("ObsoleteSdkInt")
    private fun triggerHapticFeedback() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(70)
            }
        }
    }

    private fun isEmergencyOrUtility(context: Context, rawNumber: String, normNumber: String): Boolean {
        if (isEmergencyNumber(rawNumber)) return true
        val safeList = setOf("100", "180", "181", "190", "191", "192", "193", "194", "197", "198", "199", "153", "112", "911")
        return safeList.any { safeNum -> normNumber == safeNum || normNumber.endsWith(safeNum) }
    }

    private fun hasContactPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    private fun isContact(context: Context, number: String): Boolean {
        if (number.isBlank()) return false
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            context.contentResolver.query(uri, projection, null, null, null)?.use { if (it.moveToFirst()) return true }
        } catch (e: Exception) { Log.e("VELVET_SRV", "Erro contato: $e") }
        return false
    }

    private fun extractDDD(number: String): String? {
        val cleanNumber = number.replace("+55", "").filter { it.isDigit() }
        return if (cleanNumber.length >= 2) cleanNumber.substring(0, 2) else null
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @SuppressLint("MissingPermission")
    private fun showNotification(number: String, reason: String) {
        val channelId = "BLOCK_EVENTS_CHANNEL"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val lastTime = lastBlockTimes[number] ?: 0L
        val isSpamming = (currentTimeMillis - lastTime) < 60000
        lastBlockTimes[number] = currentTimeMillis

        // Silenciamento Noturno também é influenciado pela permissão de Trial
        val isNightModeSettingEnabled = runBlocking { userSettings.nightModeFlow.first() }
        val isTrialAllowed = runBlocking { userSettings.isFeatureAllowed() }

        // Se o Trial expirou, o app ignora o modo noturno para "incomodar" e incentivar a compra
        val applyNightSilence = isNightModeSettingEnabled && (hour >= 22 || hour < 6) && isTrialAllowed

        val isHighRisk = reason.contains("Blacklist", ignoreCase = true) || reason.contains("Spam", ignoreCase = true)
        val accentColor = if (isHighRisk) Color.parseColor("#EF4444") else ContextCompat.getColor(this, R.color.royal_cyan)

        val priority = when {
            applyNightSilence -> NotificationCompat.PRIORITY_MIN
            isSpamming -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_HIGH
        }

        val remoteViews = RemoteViews(packageName, R.layout.notification_block_success).apply {
            setTextViewText(R.id.notif_block_number, if (isHighRisk) "ALERTA: Número Barrado" else "Escudo: Chamada Barrada")
            setTextViewText(R.id.notif_block_reason, "$number ($reason)")
            val glow = if (isHighRisk) R.drawable.bg_icon_red_alert else R.drawable.bg_icon_cyan_glow
            setInt(R.id.icon_container, "setBackgroundResource", glow)
            setInt(R.id.notif_arrow, "setColorFilter", accentColor)
        }

        val individualBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield_large)
            .setCustomContentView(remoteViews)
            .setGroup(GROUP_KEY_BLOCKS)
            .setPriority(priority)
            .setColor(accentColor)
            .setVibrate(if (applyNightSilence || isSpamming) longArrayOf(0) else longArrayOf(0, 100))
            .setAutoCancel(true)
            .build()

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