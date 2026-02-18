package blu.macaw.velvetwall.service.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetDark

@Composable
fun HelpStep(icon: ImageVector, title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp)) {
        Box(
            modifier = Modifier.size(40.dp).background(VelvetDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = RoyalCyan, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(desc, color = Color.Gray, fontSize = 13.sp)
        }
    }
}