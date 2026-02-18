package blu.macaw.velvetwall.service.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blu.macaw.velvetwall.ui.MainViewModel

@Composable
fun PermissionStatusCard(viewModel: MainViewModel) {
    val isEnabled by viewModel.isServiceActive.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) Color(0xFF065F46) else Color(0xFF7F1D1D)
        )
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.Shield else Icons.Default.GppBad,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(if (isEnabled) "PROTEÇÃO ATIVA" else "ESCUDO EM PAUSA", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Status baseado nas permissões do sistema", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}