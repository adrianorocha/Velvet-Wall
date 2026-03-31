package blu.macaw.velvetwall.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import blu.macaw.velvetwall.R // Import necessário para acessar o R.raw
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import kotlin.random.Random


data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val color: Color,
    val speed: Float,
    val rotation: Float,
    val size: Float
)

@Composable
fun ConfettiEffect() {
    val context = LocalContext.current

    // 1. GERENCIAMENTO DO SOM (Ciclo de Vida)
    // Usamos o DisposableEffect para garantir que o som pare e a memória seja liberada
    DisposableEffect(Unit) {
        val mediaPlayer = MediaPlayer.create(context, R.raw.applause)
        mediaPlayer.start() // Toca os aplausos assim que o componente entra na tela

        onDispose {
            mediaPlayer.stop()
            mediaPlayer.release() // Importante para não dar leak de memória no A56
        }
    }

    // 2. LÓGICA DO CONFETE (Seu código original otimizado)
    val pieces = remember {
        List(70) { // Aumentei para 70 para ficar mais festivo
            ConfettiPiece(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -1f,
                color = listOf(RoyalCyan, Color.White, Color(0xFFFFD700), Color(0xFF00E5FF)).random(),
                speed = Random.nextFloat() * 12f + 6f,
                rotation = Random.nextFloat() * 360f,
                size = Random.nextFloat() * 12f + 8f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ConfettiTransition")
    val animState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3000f, // Aumentado para o confete cair por mais tempo
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ConfettiAnimation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        pieces.forEach { piece ->
            // Cálculo da queda baseado no animState
            val currentY = (piece.y * size.height + animState * (piece.speed / 10f)) % (size.height + 100f)
            val currentX = piece.x * size.width

            // Desenha apenas se estiver dentro da área visível
            if (currentY <= size.height) {
                withTransform({
                    rotate(piece.rotation + animState / 2f, Offset(currentX, currentY))
                }) {
                    drawRect(
                        color = piece.color,
                        topLeft = Offset(currentX, currentY),
                        size = Size(piece.size, piece.size / 2f)
                    )
                }
            }
        }
    }
}