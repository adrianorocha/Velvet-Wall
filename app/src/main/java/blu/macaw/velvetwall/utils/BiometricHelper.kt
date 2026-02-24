package blu.macaw.velvetwall.utils

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat

class BiometricHelper(private val activity: FragmentActivity) {

    fun showAuthPrompt(
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder() // O segredo está aqui: .Builder() e não .newBuilder()
            .setTitle("Acesso Restrito")
            .setSubtitle("Use sua biometria para ver os logs do Velvet Wall")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) // Opcional: Garante biometria forte
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}