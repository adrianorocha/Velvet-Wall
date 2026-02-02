package blu.macaw.velvetwall.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import blu.macaw.velvetwall.ui.MainViewModel
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack
import blu.macaw.velvetwall.ui.theme.VelvetDark

@Composable
fun WhitelistScreen(viewModel: MainViewModel) {
    val whitelist by viewModel.whitelist.collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color.Green, // Verde para diferenciar
                contentColor = VelvetBlack
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        },
        containerColor = VelvetBlack
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "Lista Branca (Permitidos)",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                "Estes números nunca serão bloqueados, mesmo se não estiverem na agenda.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn {
                items(whitelist) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VelvetDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(item.number, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(item.name, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            IconButton(onClick = { viewModel.removeFromWhitelist(item) }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            // Reutilizando lógica simples de Dialog
            AddNumberDialog(
                onDismiss = { showDialog = false },
                onConfirm = { num, name -> viewModel.addToWhitelist(num, name) }
            )
        }
    }
}