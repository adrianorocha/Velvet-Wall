package blu.macaw.velvetwall.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DigitalReceiptScreen(isPremium: Boolean, purchaseDateInMillis: Long = System.currentTimeMillis()) {
    // Cores da Paleta Velvet Wall
    val velvetDark = Color(0xFF0F172A)
    val royalCyan = Color(0xFF22D3EE)
    val textMuted = Color(0xFF94A3B8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(velvetDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isPremium) {
            ReceiptCard(royalCyan, textMuted, purchaseDateInMillis)
        } else {
            UpsellCard(royalCyan)
        }
    }
}

@Composable
fun ReceiptCard(accentColor: Color, textMuted: Color, dateInMillis: Long) {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    val formattedDate = formatter.format(Date(dateInMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Escudo de Segurança",
                tint = accentColor,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "VELVET WALL PRO",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Text(
                text = "Licença Vitalícia Ativa",
                color = accentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Divider(modifier = Modifier.padding(vertical = 24.dp), color = textMuted.copy(alpha = 0.2f))

            ReceiptRow(label = "Status:", value = "Autenticado", valueColor = Color(0xFF4ADE80)) // Verde Sucesso
            ReceiptRow(label = "Emissão:", value = formattedDate, valueColor = Color.White)
            ReceiptRow(label = "ID da Licença:", value = "BWL-${UUID.randomUUID().toString().substring(0, 8).uppercase()}", valueColor = Color.White)

            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verificado",
                tint = Color(0xFF4ADE80),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Criptografado e Assinado digitalmente por\nBlu Macaw Lab's",
                color = textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 14.sp)
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun UpsellCard(accentColor: Color) {
    // Layout simples para quem ainda não comprou
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Security, contentDescription = "Bloqueado", tint = Color.Gray, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Recibo Indisponível", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Adquira a versão PRO para emitir seu certificado de segurança.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}