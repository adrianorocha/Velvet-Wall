package blu.macaw.velvetwall.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack
import kotlinx.coroutines.delay

@Composable
fun LogBiometricsScreen() {
    var isScanning by remember { mutableStateOf(true) }
    var accessGranted by remember { mutableStateOf(false) }

    // Efeito de pulsação do scanner
    val infiniteTransition = rememberInfiniteTransition(label = "Scanner")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScanLine"
    )

    LaunchedEffect(Unit) {
        delay(3000) // Simula o tempo de "leitura biométrica"
        isScanning = false
        accessGranted = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BIOMETRIA DE LOGS",
            color = RoyalCyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (isScanning) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .border(2.dp, RoyalCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = RoyalCyan.copy(alpha = 0.5f),
                    modifier = Modifier.size(120.dp)
                )

                // Linha de Scanner Laser
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .offset(y = (-100).dp + (200.dp * scanY))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, RoyalCyan, Color.Transparent)
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("IDENTIFICANDO TITULAR...", color = Color.Gray, fontSize = 14.sp)
        } else if (accessGranted) {
            // LISTA DE LOGS "ELITE"
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("AMEAÇAS INTERCEPTADAS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                items(dummyLogs) { log ->
                    LogItem(log)
                }
            }
        }
    }
}

@Composable
fun LogItem(log: LogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Shield, contentDescription = null, tint = RoyalCyan, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(log.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(log.time, color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(log.status, color = RoyalCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

data class LogEntry(val title: String, val time: String, val status: String)

val dummyLogs = listOf(
    LogEntry("Chamada DDD 011 Bloqueada", "Hoje, 14:20", "INTERCEPTADO"),
    LogEntry("Tentativa de Rastreio GPS", "Hoje, 11:05", "PROTEGIDO"),
    LogEntry("Script Malicioso Identificado", "Ontem, 23:45", "ELIMINADO"),
    LogEntry("Conexão Não Autorizada", "Ontem, 20:12", "BLOQUEADO")
)