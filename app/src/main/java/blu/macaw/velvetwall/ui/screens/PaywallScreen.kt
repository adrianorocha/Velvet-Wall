package blu.macaw.velvetwall.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack

@Composable
fun PaywallScreen(
    isRestoring: Boolean,
    onBuyClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onLogsClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current // Necessário para o Toast

    val surfaceDark = Color(0xFF1E293B)
    val textMuted = Color(0xFF94A3B8)

    // Brush para o fundo da tela
    val bgBrush = Brush.verticalGradient(
        colors = listOf(VelvetBlack, Color(0xFF020617))
    )

    // Brush animado para o efeito Shimmer no botão
    val shimmerBrush = rememberAnimatedShimmerBrush(
        shimmerColor = Color.White.copy(alpha = 0.4f), // Cor do brilho
        backgroundColor = Color.Transparent // Fundo transparente para sobrepor o botão
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        // Botão de fechar
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, "Fechar", tint = textMuted)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Header com Ícone Premium
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = RoyalCyan.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, RoyalCyan.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = RoyalCyan,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Desbloqueie o Silêncio Absoluto",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Text(
                text = "Proteção de elite pela Blu Macaw Lab's.",
                color = textMuted,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            // 2. Lista de Benefícios
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    val features = listOf(
                        FeatureItem(Icons.Default.PublicOff, "Modo Paranóico (DDD)"),
                        FeatureItem(Icons.Default.VisibilityOff, "Modo Stealth Invisível"),
                        FeatureItem(Icons.Default.Fingerprint, "Biometria de Logs"),
                        FeatureItem(Icons.Default.AllInclusive, "Licença Vitalícia")
                    )

                    features.forEach { item ->
                        FeatureRow(item.icon, item.text, RoyalCyan)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // 3. Preço
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "PAGAMENTO ÚNICO",
                    color = RoyalCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "R$",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 8.dp, end = 4.dp)
                    )
                    Text(
                        "29,90",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text("Sem assinaturas. Para sempre.", color = textMuted, fontSize = 14.sp)
            }

            Spacer(Modifier.height(32.dp))

            // 4. Botão de Compra com Shimmer Animado
            Button(
                onClick = onBuyClick,
                enabled = !isRestoring,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalCyan),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                // Usamos um Box para sobrepor o efeito shimmer ao texto
                Box(contentAlignment = Alignment.Center) {
                    // O texto base do botão
                    Text(
                        text = "TORNAR-SE PRO AGORA",
                        color = VelvetBlack,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )

                    // O efeito shimmer aplicado sobre o texto usando um Box com background brush
                    // e cortado no formato do texto (simplificado aqui como um retângulo sobreposto)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp) // Mesma altura do botão
                            .background(shimmerBrush)
                    )
                }
            }

            TextButton(
                onClick = onRestoreClick,
                enabled = !isRestoring,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRestoring) {
                        // Rodinha de progresso no lugar do ícone ou ao lado do texto
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = RoyalCyan
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Buscando...", color = textMuted)
                    } else {
                        Text("Restaurar licença anterior", color = textMuted)
                    }
                }
            }
        }
    }
}

// --- Estruturas Auxiliares ---

private data class FeatureItem(val icon: ImageVector, val text: String)

@Composable
private fun FeatureRow(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 10.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Cria um Brush animado que simula um efeito de brilho passando.
 */
@Composable
fun rememberAnimatedShimmerBrush(
    shimmerColor: Color,
    backgroundColor: Color,
    durationMillis: Int = 1500
): Brush {
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f, // Valor arbitrário que cobre a largura do componente
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    return Brush.linearGradient(
        colors = listOf(
            backgroundColor,
            shimmerColor,
            backgroundColor
        ),
        start = Offset(translateAnimation - 200f, translateAnimation - 200f),
        end = Offset(translateAnimation, translateAnimation),
        tileMode = TileMode.Clamp
    )
}