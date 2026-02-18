package blu.macaw.velvetwall

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import blu.macaw.velvetwall.data.AppDatabase
import blu.macaw.velvetwall.data.CallRepository
import blu.macaw.velvetwall.data.UserSettings
import blu.macaw.velvetwall.service.AppStatusService
import blu.macaw.velvetwall.ui.MainViewModel
import blu.macaw.velvetwall.ui.MainViewModelFactory
import blu.macaw.velvetwall.ui.VelvetAppNavigation
import blu.macaw.velvetwall.ui.screens.VelvetSplashScreen
import blu.macaw.velvetwall.ui.theme.VelvetWallTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val userSettings by lazy { UserSettings(applicationContext) }

    // Factory atualizada com BillingManager (se você seguiu os passos de monetização)
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            application,
            CallRepository(applicationContext, AppDatabase.getDatabase(applicationContext)),
            userSettings // Contexto para o Billing
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicia o Serviço
        startAppService()

        // 2. Verifica se veio um comando da notificação (ex: "blacklist")
        val targetScreen = intent.getStringExtra("NAVIGATE_TO") ?: "home"

        // 3. Verifica Biometria antes de abrir
        checkBiometricsAndLoad(targetScreen)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "BLOCK_EVENTS_CHANNEL",
                "Alertas de Bloqueio",
                NotificationManager.IMPORTANCE_HIGH // Garante que a notificação "pule" na tela
            ).apply {
                description = "Avisa quando o Velvet Wall bloqueia uma chamada"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

    }

    // --- CORREÇÃO: Lidar com cliques quando o app JÁ está aberto ---
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Atualiza o intent atual

        val targetScreen = intent.getStringExtra("NAVIGATE_TO") ?: "home"
        Log.d("VelvetNav", "Novo comando recebido: $targetScreen")

        // Se veio da notificação, carregamos direto (pulando splash)
        loadUI(targetScreen, skipSplash = true)
    }

    private fun startAppService() {
        val serviceIntent = Intent(this, AppStatusService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkBiometricsAndLoad(targetScreen: String) {
        lifecycleScope.launch {
            val isBiometricEnabled = userSettings.biometricFlow.first()

            if (isBiometricEnabled) {
                authenticateUser(targetScreen)
            } else {
                // Se não tem biometria, carrega normal
                // Se targetScreen != home, pulamos o splash para ser rápido
                val skipSplash = targetScreen != "home"
                loadUI(targetScreen, skipSplash)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun authenticateUser(targetScreen: String) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Sucesso: Carrega a tela solicitada
                    loadUI(targetScreen, skipSplash = targetScreen != "home")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Erro/Cancelado: Carrega Home ou fecha, dependendo da sua lógica.
                    // Aqui carregamos a Home por segurança se falhar.
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        // Usuário cancelou
                    } else {
                        loadUI("home")
                    }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Velvet Wall")
            .setSubtitle("Autentique para acessar")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            loadUI(targetScreen)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadUI(targetScreen: String, skipSplash: Boolean = false) {
        setContent {
            VelvetWallTheme {
                // Se skipSplash for true, showSplash começa false
                var showSplash by remember { mutableStateOf(!skipSplash) }

                if (showSplash) {
                    VelvetSplashScreen {
                        showSplash = false
                    }
                } else {
                    // Passamos o targetScreen para a navegação iniciar na tela certa
                    VelvetAppNavigation(viewModel, startDestination = targetScreen)
                }
            }
        }
    }
}