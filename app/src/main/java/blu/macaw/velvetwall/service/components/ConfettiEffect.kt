package blu.macaw.velvetwall.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
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
    val pieces = remember {
        List(50) {
            ConfettiPiece(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -1f, // Começa acima da tela
                color = listOf(RoyalCyan, Color.White, Color(0xFFFFD700)).random(),
                speed = Random.nextFloat() * 10f + 5f,
                rotation = Random.nextFloat() * 360f,
                size = Random.nextFloat() * 10f + 10f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ConfettiTransition")
    val animState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "ConfettiAnimation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        pieces.forEach { piece ->
            val currentY = (piece.y * size.height + animState * (piece.speed / 10f)) % size.height
            val currentX = piece.x * size.width

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