package blu.macaw.velvetwall.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import blu.macaw.velvetwall.MainViewModel
import blu.macaw.velvetwall.data.BlockedNumber
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
                "Números Bloqueados",
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
                },
                tipoLista = "BLACKLIST"
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
                    contentDescription = "Mover para Contatos Seguros",
                    tint = Color.Green
                )            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
fun AddNumberDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit, tipoLista:String) {
    var number by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("Bloquear Novo Número") }
    var titleText2 by remember { mutableStateOf("Motivo (ex: Cobrança)") }
    var titleText3 by remember { mutableStateOf("BLOQUEAR") }

    if (tipoLista == "BLACKLIST") {
        titleText = "Bloquear Novo Número"
        titleText2 = "Motivo (ex: Cobrança)"
        titleText3 = "BLOQUEAR"
    } else {
        titleText = "Adicionar Contato Seguro"
        titleText2 = "Nome do Contato"
        titleText3 = "ADICIONAR"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelvetDark,
        title = { Text(titleText, color = RoyalCyan) },
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
                    label = { Text(titleText2) },
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
                Text(titleText3, color = VelvetBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}