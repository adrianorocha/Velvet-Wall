package blu.macaw.velvetwall.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

    LaunchedEffect(Unit) {
        soundHelper.playSuccess()
    }
    // Animação de escala para o ícone
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "IconScale"
    )
    ConfettiEffect()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack.copy(alpha = 0.95f)) // Overlay escuro elegante
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

    DisposableEffect(Unit) {
        onDispose { soundHelper.release() }
    }
}