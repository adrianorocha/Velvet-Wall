package blu.macaw.velvetwall.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import blu.macaw.velvetwall.ui.theme.RoyalCyan

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector, // R.drawable...
    val highlightColor: Color = RoyalCyan
)

val tutorialSteps = listOf(
    OnboardingStep(
        "Blindagem Ativa",
        "O Velvet Wall intercepta chamadas de telemarketing (DDD 011) antes mesmo do seu celular tocar.",
        Icons.Default.Shield
    ),
    OnboardingStep(
        "Biometria de Logs",
        "Acompanhe em tempo real cada tentativa de invasão bloqueada com nosso scanner de segurança.",
        Icons.Default.Fingerprint
    ),
    OnboardingStep(
        "Configuração Vital",
        "Para sua proteção funcionar, o Android exige que o Velvet Wall seja o app padrão de Identificação de Spam.",
        Icons.Default.Settings
    )
)
