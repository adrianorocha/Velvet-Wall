package blu.macaw.velvetwall.ui

import android.Manifest.permission
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import blu.macaw.velvetwall.MainViewModel
import blu.macaw.velvetwall.data.BlockedCallLog
import blu.macaw.velvetwall.service.components.HelpScreen
import blu.macaw.velvetwall.ui.screens.BlacklistScreen
import blu.macaw.velvetwall.ui.screens.LogBiometricsScreen
import blu.macaw.velvetwall.ui.screens.PaywallScreen
import blu.macaw.velvetwall.ui.screens.SettingsScreen
import blu.macaw.velvetwall.ui.screens.SuccessPurchaseScreen
import blu.macaw.velvetwall.ui.screens.VelvetPulsingShield
import blu.macaw.velvetwall.ui.screens.WhitelistScreen
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack
import blu.macaw.velvetwall.utils.BiometricHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val DarkBg = Color(0xFF0F172A)

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun VelvetAppNavigation(
    viewModel: MainViewModel = viewModel(),
    startDestination: String = "home"
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current

    // --- 🚨 MODO DE SIMULAÇÃO DA BLU MACAW 🚨 ---
    // Mude para 'false' quando for publicar na Play Store!
    val DEBUG_SIMULATE_TRIAL_EXPIRED = true

    // --- ESTADOS ---
    val isPremium by viewModel.isPremiumEnabled.collectAsState()
    val trialStart by viewModel.trialStartTimestamp.collectAsState()
    val showSuccess by viewModel.showSuccess.collectAsState()
    val priceState by viewModel.paywallState.collectAsState()
    val isRestoringUI by viewModel.isRestoring.collectAsState()

    // --- CONTROLES DE UI ---
    var showPaywall by rememberSaveable { mutableStateOf(false) }
    var forcePremiumState by rememberSaveable { mutableStateOf(false) }
    var isManualDebug by remember { mutableStateOf(false) }
    var showBiometricLogs by remember { mutableStateOf(false) }

    val userIsPro = isPremium || forcePremiumState

    // 🛡️ LAUNCHED EFFECT: O CORAÇÃO DA REGRA DE NEGÓCIO
    LaunchedEffect(isPremium, trialStart, showSuccess, isManualDebug) {
        // Se abriu via botão secreto, não mexe mais.
        if (isManualDebug) return@LaunchedEffect

        // Se o usuário comprou, fecha tudo.
        if (showSuccess || isPremium) {
            showPaywall = false
            return@LaunchedEffect
        }

        // ⏳ MÁQUINA DO TEMPO: Verifica se o trial expirou
        val isTrialExpired = if (DEBUG_SIMULATE_TRIAL_EXPIRED) {
            true // Simula imediatamente que os 7 dias já passaram!
        } else {
            val fourteenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
            val currentTime = System.currentTimeMillis()
            trialStart > 0 && (currentTime - trialStart) > fourteenDaysInMillis
        }

        // Se não é PRO e o tempo (ou a simulação) acabou, ABRIR PAYWALL!
        if (!userIsPro && isTrialExpired) {
            showPaywall = true
            viewModel.fetchPremiumPrice() // 💰 Adicionado! Senão ficaria preso no carregamento.
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. CONTEÚDO PRINCIPAL (SCAFFOLD)
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = DarkBg) {
                    val items = listOf(
                        NavigationItem("home", "Escudo", Icons.Default.Shield),
                        NavigationItem("blacklist", "Lista", Icons.Default.Block),
                        NavigationItem("history", "Logs", Icons.Default.History),
                        NavigationItem("settings", "Ajustes", Icons.Default.Settings),
                        NavigationItem("whitelist", "Permitir", Icons.Default.ThumbUp)
                    )

                    items.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
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
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(padding)
            ) {
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onDebugPaywall = {
                            forcePremiumState = false
                            isManualDebug = true
                            showPaywall = true
                            viewModel.fetchPremiumPrice()
                        }
                    )
                }

                composable("blacklist") { BlacklistScreen(viewModel) }
                composable("history") { HistoryScreen(viewModel) }
                composable("settings") {
                    SettingsScreen(viewModel, onNavigateToHelp = { navController.navigate("help") })
                }
                composable("whitelist") { WhitelistScreen(viewModel) }
                composable("help") {
                    HelpScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }

        // 2. A "MÁGICA" DO SLIDE UP (PAYWALL)
        AnimatedVisibility(
            visible = showPaywall && (!userIsPro || isManualDebug),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 600)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 500)
            ),
            modifier = Modifier.zIndex(10f)
        ) {
            val activity = context as? Activity
            PaywallScreen(
                priceState = priceState,
                isRestoring = isRestoringUI,
                onBuyClick = { activity?.let { viewModel.buyPremium(it) } },
                onRestoreClick = { viewModel.restorePremium(context) },
                onCloseClick = {
                    showPaywall = false
                    isManualDebug = false
                },
                onLogsClick = { showBiometricLogs = true }
            )
        }

        // NOVA CAMADA: BIOMETRIA DE LOGS
        AnimatedVisibility(
            visible = showBiometricLogs,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.zIndex(15f)
        ) {
            Box {
                LogBiometricsScreen()
                TextButton(
                    onClick = { showBiometricLogs = false },
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                ) {
                    Text("VOLTAR", color = RoyalCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut(),
            modifier = Modifier.zIndex(20f)
        ) {
            SuccessPurchaseScreen(onGetStarted = {
                viewModel.dismissSuccessAnimation()
                forcePremiumState = true
                showPaywall = false
            })
        }
    }
}/**
 * Modelo de dados para itens de navegação, mantendo o código limpo.
 */
data class NavigationItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun HomeScreen(viewModel: MainViewModel, onDebugPaywall: () -> Unit) {
    val context = LocalContext.current
    val isEnabled by viewModel.isServiceActive.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val blockMessage by viewModel.blockEvent.collectAsState(initial = "")

    // 🛡️ O PULO DO GATO: Puxando o total do banco de dados em tempo real
    val history by viewModel.history.collectAsState(initial = emptyList())
    val blockedCount = history.size

    var showDebugMenu by remember { mutableStateOf(false) }
    var debugClickCount by remember { mutableStateOf(0) }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.checkRoleStatus(context)
    }

    // Dialog do Menu Secreto
    if (showDebugMenu) {
        AlertDialog(
            onDismissRequest = { showDebugMenu = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("🛠️ Blu Macaw Debug", color = Color.White) },
            text = {
                Column {
                    Button(
                        onClick = { viewModel.resetPremiumForDebug(); showDebugMenu = false },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("RESETAR PREMIUM (VOLTAR FREE)") }

                    Button(
                        onClick = { viewModel.triggerSuccessForDebug(); showDebugMenu = false },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalCyan)
                    ) { Text("FORÇAR TELA DE SUCESSO (CONFETES)") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDebugMenu = false }) { Text("FECHAR") }
            }
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.all { it }) {
            Toast.makeText(context, "Proteção Total Ativada!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(blockMessage) {
        if (blockMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(blockMessage, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(Unit) { viewModel.checkRoleStatus(context) }

    fun hasAllPermissions(): Boolean {
        val contactPerm = ContextCompat.checkSelfPermission(context, permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val notifPerm = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        return contactPerm && notifPerm
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data ->
            Snackbar(containerColor = Color(0xFF1E293B), contentColor = RoyalCyan, snackbarData = data)
        }},
        containerColor = VelvetBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Escudo Central
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        debugClickCount++
                        if (debugClickCount >= 5) {
                            showDebugMenu = true
                            debugClickCount = 0
                        }
                    }
                )
            ) {
                Box(
                    modifier = Modifier.size(260.dp), // Diminuí um pouco para dar mais espaço ao card
                    contentAlignment = Alignment.Center
                ){
                    VelvetPulsingShield(isServiceActive = isEnabled)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                if (isEnabled) "VELVET WALL ATIVO" else "PROTEÇÃO DESATIVADA",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                if (isEnabled) "Sua privacidade está blindada pela Blu Macaw."
                else "Conceda as permissões para iniciar o bloqueio inteligente.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )

            // 🛡️ NOVO: CARD DE ESTATÍSTICA (Contador de Ameaças)
// 🛡️ NOVO: PAINEL DE TELEMETRIA (Estatística + Eficiência)
            if (isEnabled) {
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.5f), // Fundo translúcido
                    shape = RoundedCornerShape(20.dp), // Bordas mais arredondadas
                    border = BorderStroke(1.dp, RoyalCyan.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly, // Espaça as colunas igualmente
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 📊 COLUNA 1: Bloqueios Reais
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$blockedCount", // A variável do banco de dados
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = RoyalCyan,
                                        blurRadius = 10f
                                    )
                                )
                            )
                            Text(
                                text = "Neutralizadas",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // ⚡ DIVISOR DE VIDRO (Linha central)
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(48.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )

                        // 📈 COLUNA 2: Taxa de Eficiência
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981), // Verde Esmeralda (Sucesso)
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "100%",
                                    color = Color(0xFF10B981), // Verde Esmeralda (Sucesso)
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color(0xFF10B981).copy(alpha = 0.6f),
                                            blurRadius = 10f
                                        )
                                    )
                                )
                            }
                            Text(
                                text = "Eficiência do Filtro",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                // Se estiver desativado, apenas mantemos o espaço
                Spacer(Modifier.height(48.dp))
            }
            // BOTÃO DE AÇÃO PREMIUM
            Button(
                onClick = {
                    val roleManager = context.getSystemService(RoleManager::class.java)

                    when {
                        roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                roleLauncher.launch(intent)
                            }
                        }
                        !hasAllPermissions() -> {
                            val perms = mutableListOf(
                                permission.READ_CONTACTS,
                                permission.READ_PHONE_STATE,
                                permission.ANSWER_PHONE_CALLS
                            )
                            if (Build.VERSION.SDK_INT >= 33) perms.add(permission.POST_NOTIFICATIONS)
                            permissionsLauncher.launch(perms.toTypedArray())
                        }
                        else -> {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isEnabled) Color.Transparent else RoyalCyan),
                border = if (isEnabled) BorderStroke(1.dp, RoyalCyan) else null
            ) {
                if (isEnabled) {
                    Icon(Icons.Default.CheckCircle, null, tint = RoyalCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("REVISAR PROTEÇÃO", color = RoyalCyan, fontWeight = FontWeight.Bold)
                } else {
                    Text("ATIVAR ESCUDO VELVET", color = VelvetBlack, fontWeight = FontWeight.Bold)
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
    val context = LocalContext.current
    val isPremium by viewModel.isPremiumEnabled.collectAsState()
    var isAuthenticated by remember { mutableStateOf(false) }

    LaunchedEffect(isPremium) {
        if (isPremium && !isAuthenticated) {
            val activity = context as? FragmentActivity
            activity?.let {
                BiometricHelper(it).showAuthPrompt(
                    onSuccess = { isAuthenticated = true },
                    onError = { /* Opcional: Voltar para a Home se falhar */ }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack)
            .padding(16.dp)
    ) {
        // Cabeçalho Premium
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
                IconButton(onClick = { viewModel.clearEverything() }) {
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
                            fadeInSpec = tween(durationMillis = 300),
                            fadeOutSpec = tween(durationMillis = 200),
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
        containerColor = Color(0xFF0F172A),
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