package blu.macaw.velvetwall.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaywallScreen(
    onBuyClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    // Paleta Velvet Wall
    val velvetDark = Color(0xFF0F172A)
    val surfaceDark = Color(0xFF1E293B)
    val royalCyan = Color(0xFF22D3EE)
    val textMuted = Color(0xFF94A3B8)

    // Fundo com um leve degradê para dar profundidade
    val bgBrush = Brush.verticalGradient(
        colors = listOf(velvetDark, Color(0xFF020617))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        // Botão de fechar (Sutil, para manter o foco na compra)
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = textMuted)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Ícone de Destaque
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = "Premium",
                tint = royalCyan,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Título de Impacto
            Text(
                text = "Desbloqueie o Silêncio Absoluto",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Junte-se à elite protegida pela Blu Macaw Lab's.",
                color = textMuted,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Lista de Benefícios (O que ele ganha)
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    FeatureRow(Icons.Default.PublicOff, "Modo Paranóico (Bloqueio por DDD)", royalCyan)
                    FeatureRow(Icons.Default.VisibilityOff, "Modo Stealth (Invisível & Háptico)", royalCyan)
                    FeatureRow(Icons.Default.Fingerprint, "Proteção Biométrica de Logs", royalCyan)
                    FeatureRow(Icons.Default.AllInclusive, "Licença Vitalícia (Sem mensalidade)", royalCyan)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Preço e Urgência
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(2.dp, royalCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("PAGAMENTO ÚNICO", color = royalCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("R$ 29,90", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Acesso vitalício a todas as atualizações.", color = textMuted, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Call to Action Principal
            Button(
                onClick = onBuyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = royalCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "TORNAR-SE PRO AGORA",
                    color = velvetDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Botão de Restaurar Compra (Discreto, mas acessível)
            TextButton(onClick = onRestoreClick) {
                Text(
                    text = "Já comprou antes? Restaurar licença",
                    color = textMuted,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun FeatureRow(icon: ImageVector, text: String, iconColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}