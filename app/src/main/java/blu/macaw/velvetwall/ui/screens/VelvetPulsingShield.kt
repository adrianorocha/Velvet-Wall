package blu.macaw.velvetwall.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// 1. Defina a nossa cor Premium (Ciano Real)
val RoyalCyan = Color(0x00E5FF).copy(alpha = 0.8f) // Ajustado para brilho suave

@Composable
fun VelvetPulsingShield(isServiceActive: Boolean) {
    // 2. A "Stored Procedure" da Animação (Infinite Transition)
    // Se o serviço estiver inativo, não criamos a animação (otimização de bateria no S25)
    val infiniteTransition = rememberInfiniteTransition(label = "ShieldPulse")

    // Define a escala: o escudo cresce suavemente de 1.0 para 1.1 e volta
    val scaleBy by if (isServiceActive) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.1f, // Crescimento sutil
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing), // 1.5s por pulso
                repeatMode = RepeatMode.Reverse // Vai e volta suavemente
            ),
            label = "ScaleAnimation"
        )
    } else {
        // Se inativo, escala fica travada em 1.0 (não pulsa)
        rememberUpdatedState(1.0f)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center // Centraliza tudo
    ) {
        // 3. O Campo de Força (O anel que pulsa)
        // Este anel externo segue a animação de escala
        Box(
            modifier = Modifier
                .size(160.dp) // Base do anel pulsante
                .scale(scaleBy) // Aplica a animação de escala
                .clip(CircleShape)
                .background(RoyalCyan.copy(alpha = 0.2f)) // Brilho transparente interno
                .border(2.dp, RoyalCyan, CircleShape) // A borda visível
        )

        // 4. O Escudo Principal (Fixo no centro)
        // Este Box contém o ícone e é opaco para dar contraste
        Box(
            modifier = Modifier
                .size(130.dp) // Menor que o anel pulsante
                .clip(CircleShape)
                .background(Color(0xFF121212)) // Velvet Black opaco
                .border(1.dp, Color.DarkGray, CircleShape), // Borda sutil interna
            contentAlignment = Alignment.Center
        ) {
            // O ícone do escudo que você já tem (ou ic_shield_protected)
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Shield,
                contentDescription = null,
                tint = if (isServiceActive) RoyalCyan else Color.Gray, // Muda a cor se inativo
                modifier = Modifier.size(70.dp)
            )
        }
    }
}