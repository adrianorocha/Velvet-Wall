package blu.macaw.velvetwall.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RoyalCyan,
    secondary = NeonCyan,
    background = VelvetBlack,
    surface = VelvetDark,
    onPrimary = VelvetBlack,
    onBackground = TextWhite,
    onSurface = TextWhite,
    error = ErrorRed
)

@Composable
fun VelvetWallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        // typography = Typography, // Pode adicionar tipografia customizada se quiser
        content = content
    )
}