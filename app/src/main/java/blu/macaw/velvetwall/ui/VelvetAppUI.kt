package blu.macaw.velvetwall.ui

//import blu.macaw.velvetwall.ui.screens.HistoryScreen
import android.app.NotificationManager
import android.app.role.RoleManager
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import blu.macaw.velvetwall.data.BlockedCallLog
import blu.macaw.velvetwall.ui.screens.BlacklistScreen
import blu.macaw.velvetwall.ui.screens.HomeScreen
import blu.macaw.velvetwall.ui.screens.SettingsScreen
import blu.macaw.velvetwall.ui.screens.WhitelistScreen
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack
import blu.macaw.velvetwall.ui.theme.VelvetDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.fadeOut


// Cores Premium
val DarkBg = Color(0xFF0F172A)
val CyanAccent = Color(0xFF38BDF8)

val importance = NotificationManager.IMPORTANCE_HIGH

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun VelvetAppNavigation(
    viewModel: MainViewModel = viewModel(),
    startDestination: String = "home")
{
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DarkBg) {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("home") },
                    icon = { Icon(Icons.Default.Shield, null) },
                    label = { Text("Escudo") }
                )
                NavigationBarItem(
                    selected = currentRoute == "blacklist",
                    onClick = {
                        navController.navigate("blacklist") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Block, null) },
                    label = { Text("Lista") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VelvetBlack,
                        selectedTextColor = RoyalCyan,
                        indicatorColor = RoyalCyan,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("history") },
                    icon = { Icon(Icons.Default.History, null) },
                    label = { Text("Logs") }
                )

                NavigationBarItem(
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Ajustes") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VelvetBlack,
                        selectedTextColor = RoyalCyan,
                        indicatorColor = RoyalCyan,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = currentRoute == "whitelist",
                    onClick = {
                        navController.navigate("whitelist") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.ThumbUp, null) },
                    label = { Text("Permitir") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VelvetBlack,
                        selectedTextColor = RoyalCyan,
                        indicatorColor = RoyalCyan,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = startDestination, modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(viewModel) }
            composable("blacklist") { BlacklistScreen(viewModel) }
            composable("history") { HistoryScreen(viewModel) }

            composable("settings") { SettingsScreen(viewModel) }
            composable("whitelist") { WhitelistScreen(viewModel) }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isEnabled by viewModel.isServiceActive.collectAsState()

    // Launcher para pedir permissão de Role
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.checkRoleStatus(context)
    }

    LaunchedEffect(Unit) { viewModel.checkRoleStatus(context) }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isEnabled) Icons.Default.Shield else Icons.Default.GppBad,
            contentDescription = null,
            tint = if (isEnabled) CyanAccent else Color.Gray,
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            if (isEnabled) "VELVET WALL ATIVO" else "PROTEÇÃO DESATIVADA",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = {
                val roleManager = context.getSystemService(RoleManager::class.java)
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleLauncher.launch(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
        ) {
            Text("Configurar Permissões", color = DarkBg)
        }
    }
}

@Composable
fun BlacklistScreen(viewModel: MainViewModel) {
    // Coleta a lista do banco de dados
    val blacklist by viewModel.blacklist.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().background(VelvetBlack).padding(16.dp)) {
        Text(
            "Lista Negra",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn {
            items(blacklist) { item ->
                // O item da lista
                Card(
                    colors = CardDefaults.cardColors(containerColor = VelvetDark),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.number, // Número
                                style = MaterialTheme.typography.bodyLarge,
                                color = RoyalCyan,
                                fontWeight = FontWeight.Bold
                            )
                            // AQUI É ONDE DAVA O ERRO:
                            Text(
                                text = "Motivo: ${item.reason}", // Agora deve funcionar
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { viewModel.removeFromBlacklist(item) }) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }
}
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun HistoryScreen(viewModel: MainViewModel) {
    val history by viewModel.history.collectAsState(initial = emptyList())
    var selectedLog by remember { mutableStateOf<BlockedCallLog?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack)
            .padding(16.dp)
    ) {
        // Cabeçalho Premium
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Registro de Atividades",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${history.size} chamadas barradas",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            if (history.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearEverything() }) { // Usa a função de limpeza total que criamos
                    Icon(Icons.Default.DeleteSweep, null, tint = Color(0xFFEF4444))
                }
            }
        }

        if (history.isEmpty()) {
            EmptyStateHistory()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = history,
                    key = { it.id }
                ) { log ->
                    HistoryItem(
                        log = log,
                        modifier = Modifier.animateItem(
                            // 1. O fadeInSpec espera um objeto de animação, ex: tween, spring
                            fadeInSpec = tween(durationMillis = 300),

                            // 2. O fadeOutSpec também espera um objeto de animação Float
                            fadeOutSpec = tween(durationMillis = 200),

                            // 3. O placementSpec cuida do movimento dos outros itens
                            placementSpec = spring(stiffness = Spring.StiffnessLow)
                        ),
                        onClick = { selectedLog = log }
                    )
                }
            }
        }
    }

    // Dialog de Decisão
    selectedLog?.let { log ->
        DecisionDialog(
            log = log,
            onDismiss = { selectedLog = null },
            onAllow = {
                viewModel.allowFromHistory(log)
                selectedLog = null
            },
            onBlock = {
                viewModel.blockFromHistory(log)
                selectedLog = null
            }
        )
    }
}
@Composable
fun HistoryItem(
    log: BlockedCallLog,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF991B1B).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(log.number, color = Color.White, fontWeight = FontWeight.Bold)
                Text(log.blockReason, color = RoyalCyan, fontSize = 12.sp)
            }

            Text(
                text = dateFormat.format(Date(log.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

fun formatTime(timestamp: Long): String {
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

@Composable
fun EmptyStateHistory() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Ícone grande com opacidade reduzida para um visual elegante
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.3f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Nenhuma ameaça detectada.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Text(
                text = "Seu escudo está ativo e vigilante.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun DecisionDialog(
    log: BlockedCallLog,
    onDismiss: () -> Unit,
    onAllow: () -> Unit,
    onBlock: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A), // VelvetDark profundo
        title = {
            Text(
                text = log.number,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "Este número foi bloqueado anteriormente por: ${log.blockReason}.\nO que deseja fazer?",
                color = Color.Gray
            )
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text("CONFIAR", color = Color.Green, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onBlock) {
                Text("MANTER BLOQUEIO", color = Color(0xFFEF4444))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}