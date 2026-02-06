package blu.macaw.velvetwall.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import blu.macaw.velvetwall.R
import kotlinx.coroutines.delay

@Composable
fun VelvetSplashScreen(onAnimationFinished: () -> Unit) {
    // Configuração do Lottie
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_velvet_shield)) // Assumindo que está em res/raw/
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1 // Toca apenas uma vez
    )

    // Detecta o fim da animação para navegar
    LaunchedEffect(progress) {
        if (progress >= 1f && composition != null) {
            // Pequeno delay extra para o usuário apreciar o logo finalizado antes de trocar
            delay(500)
            onAnimationFinished()
        }
    }

    // O Fundo Degradê (para combinar com o logo)
    val darkGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Azul meia-noite (topo)
            Color(0xFF000000)  // Preto (base)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkGradient),
        contentAlignment = Alignment.Center
    ) {
        // A Animação Lottie
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(300.dp) // Ajuste o tamanho conforme necessário
        )
    }
}