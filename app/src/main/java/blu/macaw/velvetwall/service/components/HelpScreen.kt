package blu.macaw.velvetwall.service.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blu.macaw.velvetwall.ui.DarkBg
import blu.macaw.velvetwall.ui.MainViewModel
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack

@Composable
fun HelpScreen(viewModel: MainViewModel, onBack:()->Unit, onOpenSettings: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Scaffold(
        topBar = { HelpTopAppBar(onBack = onBack) },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "BLU MACAW LAB'S",
                color = RoyalCyan,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Central de Ajuda",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Card de Status Dinâmico
            PermissionStatusCard(viewModel)

            Spacer(Modifier.height(16.dp))

            // Botão Testar Escudo (Manda Bala!)
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.triggerTestNotification() // Dispara a notificação de teste
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border = BorderStroke(1.dp, RoyalCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.BugReport, null, tint = RoyalCyan)
                Spacer(Modifier.width(8.dp))
                Text("TESTAR ESCUDO AGORA", color = RoyalCyan, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))

            HelpStep(
                Icons.Default.Notifications,
                "Notificações Ativas",
                "Essencial para ver quem foi barrado em tempo real."
            )
            HelpStep(
                Icons.Default.Security,
                "Sobreposição",
                "Permite identificar spams durante a chamada."
            )
            HelpStep(
                Icons.Default.BatteryChargingFull,
                "Sem Restrições",
                "Evita que o sistema feche o app em segundo plano."
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "ABRIR CONFIGURAÇÕES DO ANDROID",
                    color = VelvetBlack,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpTopAppBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CENTRAL DE AJUDA",
                    style = MaterialTheme.typography.labelLarge,
                    color = RoyalCyan, // Cor definida no seu tema
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Velvet Wall",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Voltar",
                    tint = RoyalCyan
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = DarkBg, // Coerente com o Velvet Black
            titleContentColor = Color.White
        )
    )
}