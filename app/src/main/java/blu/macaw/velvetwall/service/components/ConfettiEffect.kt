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
import blu.macaw.velvetwall.R
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import kotlin.random.Random

// 1. A DEFINIÇÃO QUE ESTAVA FALTANDO (O "SCHEMA")
// Ela deve ficar aqui fora, no nível do pacote.
data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val color: Color,
    val speed: Float,
    val rotation: Float,
    val size: Float
)

@Composable
fun ConfettiEffect(
    volume: Float = 0.5f
) {
    val context = LocalContext.current

    // 2. Instância do Player (Lógica de som para o A56)
    val mediaPlayer = remember {
        try {
            MediaPlayer.create(context, R.raw.applause)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        mediaPlayer?.apply {
            setVolume(volume, volume)
            start()
        }
        onDispose {
            mediaPlayer?.apply {
                try {
                    if (isPlaying) stop()
                    release()
                } catch (e: Exception) { }
            }
        }
    }

    // 3. Lógica Visual
    val pieces = remember {
        List(70) {
            ConfettiPiece(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -1f,
                color = listOf(RoyalCyan, Color.White, Color(0xFFFFD700)).random(),
                speed = Random.nextFloat() * 12f + 6f,
                rotation = Random.nextFloat() * 360f,
                size = Random.nextFloat() * 12f + 8f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ConfettiTransition")
    val animState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3000f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ConfettiAnimation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        pieces.forEach { piece ->
            val currentY = (piece.y * size.height + animState * (piece.speed / 10f)) % (size.height + 100f)
            val currentX = piece.x * size.width

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