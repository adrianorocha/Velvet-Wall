package blu.macaw.velvetwall

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope // Importante
import blu.macaw.velvetwall.data.AppDatabase
import blu.macaw.velvetwall.data.CallRepository
import blu.macaw.velvetwall.data.UserSettings // Importante
import blu.macaw.velvetwall.service.AppStatusService
import blu.macaw.velvetwall.ui.MainViewModel
import blu.macaw.velvetwall.ui.MainViewModelFactory
import blu.macaw.velvetwall.ui.VelvetAppNavigation
import blu.macaw.velvetwall.ui.theme.VelvetWallTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    // Instancia o UserSettings
    private val userSettings by lazy { UserSettings(applicationContext) }

    // Passa ele para a Factory
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            CallRepository(applicationContext, AppDatabase.getDatabase(applicationContext)),
            userSettings
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verifica se a Biometria está ativada nas configurações
        lifecycleScope.launch {
            // Pega o valor salvo no DataStore (true ou false)
            val isBiometricEnabled = userSettings.biometricFlow.first()

            if (isBiometricEnabled) {
                authenticateUser()
            } else {
                loadUI() // Se estiver desligado, entra direto
            }
        }
        val serviceIntent = Intent(this, AppStatusService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Lógica para navegação por atalho (Se clicou em "Ver Lista" na notificação)
        val navigateTo = intent.getStringExtra("NAVIGATE_TO")
        if (navigateTo == "blacklist") {
            // Aqui você precisaria passar esse argumento para o Navigation,
            // mas como a biometria bloqueia o fluxo, deixamos carregar normal por enquanto.
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun authenticateUser() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    loadUI()
                }

                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Se der erro crítico, não carrega a UI ou fecha o app
                    // Aqui mantemos loadUI() para facilitar seus testes,
                    // mas em produção você usaria finish()
                    loadUI()
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
            loadUI()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadUI() {
        setContent {
            VelvetWallTheme {
                VelvetAppNavigation(viewModel)
            }
        }
    }
}