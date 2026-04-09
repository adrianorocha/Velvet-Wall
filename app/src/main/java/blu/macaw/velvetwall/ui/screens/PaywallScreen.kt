package blu.macaw.velvetwall.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack

// 1. A MÁQUINA DE ESTADOS DO PREÇO (data object)
sealed class PaywallPriceState {
    data object Loading : PaywallPriceState()
    data class Active(
        val currentPrice: String,
        val originalPrice: String? = null,
        val discountTag: String? = null
    ) : PaywallPriceState()

    data object Error : PaywallPriceState()
}

@Composable
fun PaywallScreen(
    priceState: PaywallPriceState = PaywallPriceState.Loading,
    isRestoring: Boolean,
    onBuyClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onLogsClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val surfaceDark = Color(0xFF0F172A)
    val textMuted = Color(0xFF94A3B8)
    val glowCyan = RoyalCyan.copy(alpha = 0.6f)
    val glowGold = Color(0xFFFFD700)

    // Gradiente de Fundo com uma "Aura" central
    val bgBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF06182B), VelvetBlack),
        center = Offset(500f, 500f),
        radius = 1200f
    )

    // Pincéis de Animação
    val priceShimmerBrush = rememberAnimatedShimmerBrush(
        shimmerColor = RoyalCyan.copy(alpha = 0.3f),
        backgroundColor = Color.White,
        durationMillis = 2000
    )
    val shimmerBrush = rememberAnimatedShimmerBrush(
        shimmerColor = Color.White.copy(alpha = 0.5f),
        backgroundColor = Color.Transparent
    )

    // 🎯 NOVO: Pincéis Dourados para a Tag de Promoção
    val tagBorderShimmerBrush = rememberAnimatedShimmerBrush(
        shimmerColor = Color.White, // A luz branca que vai correr na borda
        backgroundColor = glowGold.copy(alpha = 0.3f), // O fundo da borda
        durationMillis = 2000
    )
    val tagTextShimmerBrush = rememberAnimatedShimmerBrush(
        shimmerColor = Color.White,
        backgroundColor = glowGold,
        durationMillis = 2000
    )

    val heartbeatModifier = rememberAnimatedHeartbeatModifier()
    val shakeModifier = rememberAnimatedShakeModifier()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        // Botão Fechar Discreto
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = textMuted)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 🛡️ Ícone Hero com Efeito Neon
            Surface(
                modifier = Modifier.size(90.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.4f),
                border = BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(listOf(RoyalCyan, Color.Transparent))
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = RoyalCyan,
                        modifier = Modifier
                            .size(54.dp)
                            .graphicsLayer {
                                shadowElevation = 20f
                                ambientShadowColor = RoyalCyan
                                spotShadowColor = RoyalCyan
                            }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Título com Sombra Brilhante (Glow)
            Text(
                text = "Silêncio Absoluto",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    shadow = Shadow(color = glowCyan, blurRadius = 25f),
                    textAlign = TextAlign.Center
                )
            )

            Text(
                text = "Eleve sua privacidade ao nível máximo.\nProteção de Elite pela Blu Macaw Lab's.",
                color = textMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 12.dp)
            )

            Spacer(Modifier.height(32.dp))

            // 💎 Card de Features (Glassmorphism Effect)
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceDark.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    val features = listOf(
                        FeatureItem(Icons.Default.PublicOff, "Modo Paranóico (DDD)"),
                        FeatureItem(Icons.Default.VisibilityOff, "Modo Stealth Invisível"),
                        FeatureItem(Icons.Default.Fingerprint, "Biometria de Logs"),
                        FeatureItem(Icons.Default.AllInclusive, "Licença Vitalícia PRO")
                    )

                    features.forEach { item ->
                        FeatureRow(
                            icon = item.icon,
                            text = item.text,
                            color = RoyalCyan,
                            onClick = if (item.text.contains("Biometria")) onLogsClick else null
                        )
                    }
                }
            }

            Spacer(Modifier.height(30.dp))

            // 💰 ZONA DE PREÇO DINÂMICA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = priceState,
                    label = "PriceAnimation"
                ) { state ->
                    when (state) {
                        is PaywallPriceState.Loading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = RoyalCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Acessando Google Play...",
                                    color = textMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        is PaywallPriceState.Error -> {
                            Text(
                                "Falha na conexão segura.",
                                color = Color(0xFFFF4D4D),
                                fontSize = 14.sp
                            )
                        }

                        is PaywallPriceState.Active -> {
                            val isPromo = state.discountTag != null

                            // 🎯 A MÁGICA DO OVERLAP: Um Box que alinha tudo no topo-centro
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                // 1. O CONTAINER PRINCIPAL (A caixa azul-escura com o preço)
                                Surface(
                                    color = Color(0xFF020617).copy(alpha = 0.4f), // Fundo escuro para destacar o neon
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, RoyalCyan.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .padding(top = 12.dp) // ⬅️ O SEGREDO: Empurra a caixa pra baixo para a Tag "montar" em cima
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(
                                            top = 28.dp,
                                            bottom = 20.dp
                                        ) // Espaço no topo para não colar na Tag
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (state.originalPrice != null) {
                                                Text(
                                                    text = state.originalPrice,
                                                    color = Color(0xFFEF4444).copy(alpha = 0.8f),
                                                    fontSize = 18.sp,
                                                    style = TextStyle(textDecoration = TextDecoration.LineThrough),
                                                    modifier = Modifier.padding(
                                                        bottom = 6.dp,
                                                        end = 12.dp
                                                    )
                                                )
                                            }

                                            Text(
                                                text = state.currentPrice.replace(".", ","),
                                                style = TextStyle(
                                                    fontSize = 46.sp,
                                                    fontWeight = FontWeight.Black,
                                                    brush = priceShimmerBrush,
                                                    shadow = Shadow(
                                                        color = RoyalCyan.copy(alpha = 0.3f),
                                                        blurRadius = 25f
                                                    )
                                                ),
                                                modifier = heartbeatModifier
                                            )
                                        }
                                    }
                                }

                                // 2. A TAG SOBREPOSTA (OFERTA DE LANÇAMENTO)
                                if (isPromo) {
                                    Box(
                                        modifier = Modifier
                                            .then(shakeModifier) // Continua chacoalhando suavemente
                                            // 🛡️ CRUCIAL: O fundo precisa ser sólido (Color(0xFF06182B))
                                            // para "cortar" a linha azul da caixa que está atrás.
                                            .background(Color(0xFF06182B), RoundedCornerShape(50))
                                            .border(
                                                BorderStroke(1.5.dp, tagBorderShimmerBrush),
                                                RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 18.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = state.discountTag ?: "OFERTA DE LANÇAMENTO",
                                            style = TextStyle(
                                                brush = tagTextShimmerBrush,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.5.sp,
                                                shadow = Shadow(
                                                    color = glowGold.copy(alpha = 0.8f),
                                                    offset = Offset(0f, 0f),
                                                    blurRadius = 15f
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 🚀 BOTÃO CTA (Call to Action) MAXIMIZADO
            val isBuyEnabled = !isRestoring && priceState is PaywallPriceState.Active

            Button(
                onClick = onBuyClick,
                enabled = isBuyEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalCyan,
                    disabledContainerColor = surfaceDark
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 10.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "DESBLOQUEAR ACESSO VITALÍCIO",
                        color = if (isBuyEnabled) Color(0xFF020617) else textMuted,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )

                    if (isBuyEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(shimmerBrush)
                        )
                    }
                }
            }

            // Gatilho de Confiança
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Pagamento 100% seguro via Google Play",
                    color = textMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = onRestoreClick,
                enabled = !isRestoring
            ) {
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = RoyalCyan
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Sincronizando...", color = textMuted)
                } else {
                    Text("Já sou PRO? Restaurar licença.", color = textMuted.copy(alpha = 0.8f))
                }
            }
        }
    }
}

private data class FeatureItem(val icon: ImageVector, val text: String)

@Composable
private fun FeatureRow(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(Color.White.copy(alpha = 0.03f))
            .padding(vertical = 12.dp, horizontal = 12.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 12.dp)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ShimmerTranslate"
    )
    return Brush.linearGradient(
        colors = listOf(backgroundColor, shimmerColor, backgroundColor),
        start = Offset(translateAnimation - 200f, translateAnimation - 200f),
        end = Offset(translateAnimation, translateAnimation),
        tileMode = TileMode.Clamp
    )
}

@Composable
fun rememberAnimatedHeartbeatModifier(
    durationMillis: Int = 1000,
    lubScale: Float = 1.12f,
    dubScale: Float = 1.18f,
    lubJump: Dp = (-3).dp,
    dubJump: Dp = (-5).dp
): Modifier {
    val transition = rememberInfiniteTransition(label = "HeartbeatTransition")
    val density = LocalDensity.current

    val lubJumpPx = with(density) { lubJump.toPx() }
    val dubJumpPx = with(density) { dubJump.toPx() }
    val intermediateJumpPx = with(density) { (-1).dp.toPx() }

    val scaleAnim by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = durationMillis
                1.0f at 0; 1.0f at 100; lubScale at 200; 1.05f at 300; dubScale at 450; 1.0f at 600; 1.0f at durationMillis
            }
        ),
        label = "HeartbeatScale"
    )

    val translationAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = durationMillis
                0f at 0; 0f at 100; lubJumpPx at 200; intermediateJumpPx at 300; dubJumpPx at 450; 0f at 600; 0f at durationMillis
            }
        ),
        label = "HeartbeatJump"
    )

    return Modifier.graphicsLayer {
        scaleX = scaleAnim
        scaleY = scaleAnim
        translationY = translationAnim
    }
}

@Composable
fun rememberAnimatedShakeModifier(
    durationMillis: Int = 2500,
    shakeIntensity: Dp = 4.dp
): Modifier {
    val transition = rememberInfiniteTransition(label = "ShakeTransition")
    val density = LocalDensity.current
    val shakeIntensityPx = with(density) { shakeIntensity.toPx() }

    val translationXAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = durationMillis
                0f at 0; 0f at 1900
                -shakeIntensityPx at 1950
                shakeIntensityPx at 2050
                -(shakeIntensityPx / 1.5f) at 2150
                (shakeIntensityPx / 2f) at 2250
                0f at 2350
                0f at durationMillis
            }
        ),
        label = "ShakeTranslationX"
    )

    return Modifier.graphicsLayer {
        translationX = translationXAnim
    }
}