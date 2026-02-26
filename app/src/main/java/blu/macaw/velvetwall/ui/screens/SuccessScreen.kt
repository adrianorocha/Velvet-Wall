package blu.macaw.velvetwall.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blu.macaw.velvetwall.ui.components.ConfettiEffect
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack
import blu.macaw.velvetwall.utils.VelvetSoundHelper

@Composable
fun SuccessPurchaseScreen(onGetStarted: () -> Unit) {
    val context = LocalContext.current
    val soundHelper = remember { VelvetSoundHelper(context) }

    // Gatilho para a animação começar
    var animationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        soundHelper.playSuccess()
        animationStarted = true // Dispara o crescimento do ícone
    }

    // Animação de escala: agora ela vai de 0f para 1f
    val scale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "IconScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Confetes no fundo da camada de sucesso


        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(VelvetBlack) // OPACO: Para esconder o Paywall atrás 100%
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícone Animado
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(120.dp).scale(scale),
                    shape = CircleShape,
                    color = RoyalCyan.copy(alpha = 0.2f)
                ) {}
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = RoyalCyan,
                    modifier = Modifier.size(80.dp).scale(scale)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Bem-vindo à Elite!",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "O Velvet Wall PRO agora está ativo.\nSua privacidade está blindada para sempre.",
                color = Color.Gray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("COMEÇAR AGORA", color = VelvetBlack, fontWeight = FontWeight.Bold)
            }
        }
        ConfettiEffect()
    }

    DisposableEffect(Unit) {
        onDispose { soundHelper.release() }
    }
}