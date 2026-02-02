package blu.macaw.velvetwall.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import blu.macaw.velvetwall.data.BlockedNumber
import blu.macaw.velvetwall.ui.MainViewModel
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack
import blu.macaw.velvetwall.ui.theme.VelvetDark

@Composable
fun BlacklistScreen(viewModel: MainViewModel) {
    // Coleta a lista do banco em tempo real
    val blacklist by viewModel.blacklist.collectAsState(initial = emptyList())

    // Estado para controlar se a janela de adicionar está visível
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = RoyalCyan,
                contentColor = VelvetBlack
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Número")
            }
        },
        containerColor = VelvetBlack // Fundo da tela inteira
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Lista Negra",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (blacklist.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum número bloqueado.", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(blacklist) { item ->
                        BlacklistItem(
                            item = item,
                            onDelete = { viewModel.removeFromBlacklist(item) },
                            onPromote = { viewModel.moveBlackToWhite(item) } // Nova ação
                        )
                    }
                }
            }
        }

        // Janela de Adicionar (Dialog)
        if (showDialog) {
            AddNumberDialog(
                onDismiss = { showDialog = false },
                onConfirm = { number, reason ->
                    viewModel.addToBlacklist(number, reason)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun BlacklistItem(item: BlockedNumber,
                  onDelete: () -> Unit,
                  onPromote: () -> Unit) {
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
                Icon(Icons.Default.Block, null, tint = RoyalCyan, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = item.number,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            IconButton(onClick = onPromote) {
                Icon(
                    Icons.Default.ThumbUp,
                    contentDescription = "Mover para Lista Branca",
                    tint = Color.Green
                )            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
fun AddNumberDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var number by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelvetDark,
        title = { Text("Bloquear Novo Número", color = RoyalCyan) },
        text = {
            Column {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Número (ex: 11999990000)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RoyalCyan,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo (ex: Cobrança)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RoyalCyan,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (number.isNotEmpty()) onConfirm(number, reason.ifEmpty { "Manual" }) },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalCyan)
            ) {
                Text("BLOQUEAR", color = VelvetBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}