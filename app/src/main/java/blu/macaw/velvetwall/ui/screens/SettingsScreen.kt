package blu.macaw.velvetwall.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import blu.macaw.velvetwall.service.components.SettingsSwitchItem
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToHelp: () -> Unit // Callback para a HelpScreen que você perguntou onde chamar
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Estados do ViewModel
    val blockPrivate by viewModel.blockPrivate.collectAsState(initial = true)
    val blockUnknown by viewModel.blockUnknown.collectAsState(initial = false)
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState(initial = true)
    val biometricEnabled by viewModel.biometricEnabled.collectAsState(initial = true)
    val isNightModeEnabled by viewModel.nightModeEnabled.collectAsState()
    val selectedDays by viewModel.cleanupDays.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Cabeçalho
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
        )

        // 1. INTELIGÊNCIA DE BLOQUEIO
        SettingsGroup("Inteligência de Bloqueio") {
            SettingsSwitchItem(
                icon = Icons.Default.NoEncryption,
                title = "Bloquear Números Privados",
                subtitle = "Rejeitar chamadas com ID Oculto",
                checked = blockPrivate,
                onCheckedChange = { viewModel.setBlockPrivate(it) }
            )
            SettingsSwitchItem(
                icon = Icons.Default.Contacts,
                title = "Bloquear Desconhecidos",
                subtitle = "Permitir apenas contatos da agenda",
                checked = blockUnknown,
                onCheckedChange = { viewModel.setBlockUnknown(it) }
            )
        }

        // 2. PRIVACIDADE E CONFORTO (Integração do Modo Noturno)
        SettingsGroup("Privacidade & Conforto") {
            SettingsSwitchItem(
                icon = Icons.Default.NotificationsActive,
                title = "Notificações",
                subtitle = "Alertar quando uma chamada for barrada",
                checked = notificationsEnabled,
                onCheckedChange = { viewModel.setNotifications(it) }
            )
            SettingsSwitchItem(
                icon = Icons.Default.NightsStay,
                title = "Silenciamento Noturno",
                subtitle = "Modo silencioso entre 22:00 e 06:00",
                checked = isNightModeEnabled,
                onCheckedChange = { viewModel.toggleNightMode(it) }
            )
            SettingsSwitchItem(
                icon = Icons.Default.Fingerprint,
                title = "Proteção Biométrica",
                subtitle = "Exigir biometria ao abrir o app",
                checked = biometricEnabled,
                onCheckedChange = { viewModel.setBiometric(it) }
            )
        }

        // 3. MANUTENÇÃO E LOGS
        SettingsGroup("Manutenção de Logs") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Limpeza Automática", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Apagar logs com mais de $selectedDays dias", color = Color.Gray, fontSize = 12.sp)
                    }

                    Box {
                        Surface(
                            onClick = { expanded = true },
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, RoyalCyan.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$selectedDays dias", color = RoyalCyan, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ArrowDropDown, null, tint = RoyalCyan)
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            listOf(7, 15, 30, 90).forEach { days ->
                                DropdownMenuItem(
                                    text = { Text("$days dias", color = Color.White) },
                                    onClick = {
                                        viewModel.updateCleanupSettings(days)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. SUPORTE E SISTEMA
        SettingsGroup("Suporte & Sistema") {
            SettingsClickableItem(
                icon = Icons.Default.HelpOutline,
                title = "Guia de Configuração",
                subtitle = "Aprenda a manter o escudo ativo",
                onClick = onNavigateToHelp // Chamada para a sua HelpScreen
            )
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

        // 5. AÇÕES CRÍTICAS
        SettingsGroup("Ações Críticas") {
            // Botão de Limpeza Total Premium
            Button(
                onClick = {
                    viewModel.clearEverything()
                    Toast.makeText(context, "🛡️ Tudo limpo!", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, null)
                Spacer(Modifier.width(8.dp))
                Text("LIMPAR TUDO AGORA", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            SettingsClickableItem(
                icon = Icons.Default.DeleteSweep,
                title = "Limpar Apenas Histórico",
                subtitle = "Apagar registros sem resetar o app",
                onClick = { viewModel.clearHistory() },
                textColor = Color(0xFFFF5252),
                showArrow = false
            )
        }

        // Rodapé Blu Macaw
        FooterSection()
    }
}

@Composable
private fun FooterSection() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Shield, null, tint = RoyalCyan.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text("Velvet Wall", color = Color.White, fontWeight = FontWeight.Bold)
        Text("Versão 1.1.0", color = Color.Gray, fontSize = 12.sp)
        Text("Blu Macaw Lab's", color = RoyalCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}