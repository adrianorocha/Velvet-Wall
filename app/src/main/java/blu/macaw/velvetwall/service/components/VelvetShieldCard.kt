package blu.macaw.velvetwall.service.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blu.macaw.velvetwall.ui.screens.disableAutoRevokePermissions
import blu.macaw.velvetwall.ui.screens.requestUnrestrictedBattery
import blu.macaw.velvetwall.ui.theme.RoyalCyan

@Composable
fun VelvetShieldCard(context: Context) {
    val surfaceDark = Color(0xFF1E293B)
    val textMuted = Color(0xFF94A3B8)

    Surface(
        color = surfaceDark.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, RoyalCyan.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            // ITEM 1: Blindagem de Permissões
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = RoyalCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Blindagem Contínua", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Impeça o Android de desligar o escudo. Desative a chave 'Pausar atividade do app'.",
                        color = textMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp), lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { disableAutoRevokePermissions(context) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalCyan.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, RoyalCyan.copy(alpha = 0.5f))
            ) {
                Text("CONFIGURAR BLINDAGEM", color = RoyalCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))

            Spacer(Modifier.height(24.dp))

            // ITEM 2: Bateria Ilimitada
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Energia Ilimitada", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Permita o funcionamento em 2º plano sem restrições de bateria (Modo Irrestrito).",
                        color = textMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp), lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { requestUnrestrictedBattery(context) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
            ) {
                Text("REMOVER TRAVAS DE ENERGIA", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
