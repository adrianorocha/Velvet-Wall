package blu.macaw.velvetwall.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack

// 1. A MÁQUINA DE ESTADOS DO PREÇO
sealed class PaywallPriceState {
    object Loading : PaywallPriceState()
    data class Active(
        val currentPrice: String,
        val originalPrice: String? = null, // Se vier preenchido, ativa a UI de promoção
        val discountTag: String? = null    // Ex: "OFERTA DE LANÇAMENTO"
    ) : PaywallPriceState()

    object Error : PaywallPriceState()
}

@Composable
fun PaywallScreen(
    priceState: PaywallPriceState = PaywallPriceState.Loading, // Agora a tela recebe o estado
    isRestoring: Boolean,
    onBuyClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onLogsClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current

    val surfaceDark = Color(0xFF1E293B)
    val textMuted = Color(0xFF94A3B8)

    val bgBrush = Brush.verticalGradient(
        colors = listOf(VelvetBlack, Color(0xFF020617))
    )

    val shimmerBrush = rememberAnimatedShimmerBrush(
        shimmerColor = Color.White.copy(alpha = 0.4f),
        backgroundColor = Color.Transparent
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
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

            // 2. ZONA DE PREÇO DINÂMICA
            Box(modifier = Modifier.height(90.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = priceState,
                    label = "PriceAnimation"
                ) { state ->
                    when (state) {
                        is PaywallPriceState.Loading -> {
                            // Skeleton Loading
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(surfaceDark)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(surfaceDark)
                                )
                            }
                        }

                        is PaywallPriceState.Error -> {
                            Text("Verifique sua conexão.", color = Color.Red, fontSize = 14.sp)
                        }

                        is PaywallPriceState.Active -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Tag de Oferta ou Pagamento Único
                                Text(
                                    text = state.discountTag ?: "PAGAMENTO ÚNICO",
                                    color = if (state.discountTag != null) Color(0xFFFFD700) else RoyalCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    // Preço Antigo (Riscado) se for Promoção
                                    if (state.originalPrice != null) {
                                        Text(
                                            text = state.originalPrice,
                                            color = textMuted.copy(alpha = 0.6f),
                                            fontSize = 20.sp,
                                            style = androidx.compose.ui.text.TextStyle(
                                                textDecoration = TextDecoration.LineThrough
                                            ),
                                            modifier = Modifier.padding(bottom = 6.dp, end = 8.dp)
                                        )
                                    }

                                    // Preço Atual
                                    Text(
                                        text = state.currentPrice,
                                        color = Color.White,
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Text(
                                    "Sem assinaturas. Para sempre.",
                                    color = textMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 3. TRAVA DO BOTÃO (Só habilita se carregou o preço)
            val isBuyEnabled = !isRestoring && priceState is PaywallPriceState.Active

            Button(
                onClick = onBuyClick,
                enabled = isBuyEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalCyan,
                    disabledContainerColor = surfaceDark
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "TORNAR-SE PRO AGORA",
                        color = if (isBuyEnabled) VelvetBlack else textMuted,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )

                    if (isBuyEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(shimmerBrush)
                        )
                    }
                }
            }

            TextButton(
                onClick = onRestoreClick,
                enabled = !isRestoring,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRestoring) {
                        CircularProgressIndicator(
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

@Composable
fun rememberAnimatedShimmerBrush(
    shimmerColor: Color,
    backgroundColor: Color,
    durationMillis: Int = 1500
): Brush {
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
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