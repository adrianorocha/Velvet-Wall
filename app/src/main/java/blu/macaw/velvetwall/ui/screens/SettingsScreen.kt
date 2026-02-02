package blu.macaw.velvetwall.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blu.macaw.velvetwall.ui.MainViewModel
import blu.macaw.velvetwall.ui.components.SettingsClickableItem
import blu.macaw.velvetwall.ui.components.SettingsGroup
import blu.macaw.velvetwall.ui.components.SettingsSwitchItem
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // CONEXÃO COM DADOS REAIS:
    val blockPrivate by viewModel.blockPrivate.collectAsState(initial = true)
    val blockUnknown by viewModel.blockUnknown.collectAsState(initial = false)
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState(initial = true)
    val biometricEnabled by viewModel.biometricEnabled.collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            "Configurações",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
        )

        SettingsGroup("Inteligência de Bloqueio") {
            SettingsSwitchItem(
                icon = Icons.Default.NoEncryption,
                title = "Bloquear Números Privados",
                subtitle = "Rejeitar chamadas sem identificação (ID Oculto)",
                checked = blockPrivate,
                onCheckedChange = { viewModel.setBlockPrivate(it) } // Salva no DataStore
            )
            SettingsSwitchItem(
                icon = Icons.Default.Contacts,
                title = "Bloquear Desconhecidos",
                subtitle = "Permitir APENAS contatos da agenda",
                checked = blockUnknown,
                onCheckedChange = { viewModel.setBlockUnknown(it) }
            )
        }

        SettingsGroup("Sistema & Privacidade") {
            SettingsSwitchItem(
                icon = Icons.Default.NotificationsActive,
                title = "Notificações",
                subtitle = "Alertar quando uma chamada for barrada",
                checked = notificationsEnabled,
                onCheckedChange = { viewModel.setNotifications(it) }
            )
            SettingsSwitchItem(
                icon = Icons.Default.Fingerprint,
                title = "Proteção Biométrica",
                subtitle = "Exigir FaceID/TouchID ao abrir o app",
                checked = biometricEnabled,
                onCheckedChange = { viewModel.setBiometric(it) }
            )
            // ... resto do código igual ...
            SettingsClickableItem(
                icon = Icons.Default.SettingsApplications,
                title = "Permissões do Android",
                subtitle = "Gerenciar permissões de sistema",
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }

        SettingsGroup("Dados") {
            SettingsClickableItem(
                icon = Icons.Default.DeleteSweep,
                title = "Limpar Histórico",
                subtitle = "Apagar todos os logs de bloqueio",
                onClick = {
                    viewModel.clearHistory()
                    Toast.makeText(context, "Histórico limpo", Toast.LENGTH_SHORT).show()
                },
                textColor = Color(0xFFFF5252),
                showArrow = false
            )
        }

        // Rodapé
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Shield, null, tint = RoyalCyan.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text("Velvet Wall", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Versão 1.1.0 (Persistência Ativa)", color = Color.Gray, fontSize = 12.sp)
            Text("Desenvolvido por Blu Macaw", color = RoyalCyan, fontSize = 12.sp)
        }
    }
}