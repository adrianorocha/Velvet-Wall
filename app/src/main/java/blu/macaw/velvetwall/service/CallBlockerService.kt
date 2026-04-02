package blu.macaw.velvetwall.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
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
import com.android.billingclient.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

        // 1. Identificação de Número Oculto
        val isHidden = rawNumber.isEmpty() || rawNumber.isBlank() || rawNumber.equals("private", ignoreCase = true)

        // 2. 🛡️ O FILTRO DA BLU MACAW: Normalização Perfeita
        val normalizedNumber = if (isHidden) "" else normalizePhoneNumber(rawNumber)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Carregar preferências do DataStore
                val isPremium = userSettings.isPremiumFlow.first()
                val isFeatureAllowed = userSettings.isFeatureAllowed()

                val isParanoidActive = userSettings.paranoidModeFlow.first()
                val myDDD = userSettings.userLocalDDDFlow.first()
                val blockPrivate = userSettings.blockPrivateFlow.first()
                val blockUnknown = userSettings.blockUnknownFlow.first()
                val blockedDDDs = userSettings.blockedDDDsFlow.first()

                var shouldBlock = false
                var blockReason = ""
                var isTrialFeature = false

                // Checagens
                val incomingDDD = extractDDD(normalizedNumber)
                val isContact = if (!isHidden) isContact(applicationContext, rawNumber) else false

                // 3. Hierarquia de Bloqueio
                when {
                    isEmergencyOrUtility(applicationContext, rawNumber, normalizedNumber) ||
                            repository.isWhitelisted(normalizedNumber) -> {
                        respondToCall(callDetails, CallResponse.Builder().build())
                        return@launch
                    }

                    repository.shouldBlockCall(normalizedNumber) -> {
                        shouldBlock = true; blockReason = "Interceptados"
                    }

                    incomingDDD != null && blockedDDDs.contains(incomingDDD) -> {
                        shouldBlock = true; blockReason = "DDD $incomingDDD Bloqueado"; isTrialFeature = true
                    }

                    isParanoidActive && myDDD.isNotEmpty() && !isContact && incomingDDD != myDDD -> {
                        shouldBlock = true; blockReason = "Modo Paranóico"; isTrialFeature = true
                    }

                    blockUnknown && !isHidden && !isContact -> {
                        shouldBlock = true; blockReason = "Fora da Agenda"; isTrialFeature = true
                    }

                    isHidden && blockPrivate -> {
                        shouldBlock = true; blockReason = "Número Privado"
                    }
                }

                // 4. Execução Final
                if (shouldBlock) {
                    if (isTrialFeature && !isFeatureAllowed) {
                        showUpsellNotification(rawNumber.ifEmpty { "Privado" }, blockReason)
                        respondToCall(callDetails, CallResponse.Builder().build())
                    } else {
                        if (isTrialFeature && !isPremium) userSettings.startTrialIfNecessary()

                        // Agora usamos a função com 'suspend' (sem runBlocking)
                        blockAndNotify(callDetails, rawNumber.ifEmpty { "Privado" }, blockReason, isPremium)
                        repository.logBlockedCall(rawNumber.ifEmpty { "Privado" }, blockReason)
                    }
                } else {
                    respondToCall(callDetails, CallResponse.Builder().build())
                }
            } catch (e: Exception) {
                Log.e("VELVET_SRV", "Erro no processamento da chamada: ${e.message}")
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }

    /**
     * 🧹 Limpa a sujeira da operadora (Espaços, +, DDI 55, zero do DDD)
     */
    private fun normalizePhoneNumber(rawNumber: String): String {
        var clean = rawNumber.replace(Regex("[^0-9]"), "")
        if (clean.isEmpty()) return rawNumber

        if (clean.startsWith("55") && clean.length >= 12) {
            clean = clean.substring(2)
        }
        if (clean.startsWith("0") && clean.length >= 11) {
            clean = clean.substring(1)
        }
        return clean
    }

    /**
     * Extrai o DDD agora que o número está perfeitamente limpo
     */
    private fun extractDDD(normalizedNumber: String): String? {
        if (normalizedNumber.isEmpty()) return null
        return if (normalizedNumber.length >= 10) normalizedNumber.substring(0, 2) else null
    }

    // ⚡ Transformado em 'suspend' para ler do banco sem travar a thread
    private suspend fun blockAndNotify(callDetails: Call.Details, number: String, reason: String, isPremium: Boolean) {
        val isStealthActive = userSettings.stealthModeFlow.first()
        val isNotifEnabled = userSettings.notificationsFlow.first()

        val actuallyStealth = isStealthActive && isPremium

        if (actuallyStealth) {
            triggerHapticFeedback()
        }

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

    // ⚡ Transformado em 'suspend' para ler do banco sem travar a thread
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @SuppressLint("MissingPermission")
    private suspend fun showNotification(number: String, reason: String) {
        val channelId = "BLOCK_EVENTS_CHANNEL"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val lastTime = lastBlockTimes[number] ?: 0L
        val isSpamming = (currentTimeMillis - lastTime) < 60000
        lastBlockTimes[number] = currentTimeMillis

        val isNightModeSettingEnabled = userSettings.nightModeFlow.first()
        val isTrialAllowed = userSettings.isFeatureAllowed()

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

    private fun showUpsellNotification(number: String, reason: String) {
        val isDebug = (0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE))
        val isTester = BuildConfig.VERSION_NAME.contains("alpha", ignoreCase = true) || BuildConfig.VERSION_NAME.contains("beta", ignoreCase = true)
        if (isDebug || isTester) return

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, "BLOCK_EVENTS_CHANNEL")
            .setSmallIcon(R.drawable.ic_shield_large)
            .setContentTitle("Spam Detectado: $number")
            .setContentText("O Trial do $reason expirou. Ative o PRO para barrar!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(Color.parseColor("#FACC15"))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), builder)
    }

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

    private fun isContact(context: Context, number: String): Boolean {
        if (number.isBlank()) return false
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            context.contentResolver.query(uri, projection, null, null, null)?.use { if (it.moveToFirst()) return true }
        } catch (e: Exception) { Log.e("VELVET_SRV", "Erro contato: $e") }
        return false
    }
}