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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    // --- 🚨 MODO DE SIMULAÇÃO (Falso para Produção) ---
    //val DEBUG_SIMULATE_TRIAL_EXPIRED = false

    // 🎯 1. VIGIA DO CICLO DE VIDA (Verifica o relógio sempre que o app volta)
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleTrigger by remember { mutableStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    // 🛡️ MÁQUINA DO TEMPO (Disparador da Paywall)
    LaunchedEffect(isPremium, trialStart, showSuccess, isManualDebug, lifecycleTrigger) {
        if (isManualDebug) return@LaunchedEffect

        if (showSuccess || isPremium) {
            showPaywall = false
            return@LaunchedEffect
        }

        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis() + (8L * 24 * 60 * 60 * 1000L)
//        val currentTime = System.currentTimeMillis()

        val isTrialExpired = trialStart > 0 && (currentTime - trialStart) >= sevenDaysInMillis

        if (!userIsPro && isTrialExpired) {
            showPaywall = true
            viewModel.fetchPremiumPrice()
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
                        userIsPro = userIsPro,
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
                composable("settings") { SettingsScreen(viewModel, onNavigateToHelp = { navController.navigate("help") }) }
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

        // 2. PAYWALL (Z-INDEX MÁXIMO E FULL SIZE)
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
            modifier = Modifier.fillMaxSize().zIndex(100f) // 🎯 Z-Index alto e preenchimento total!
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

        // 3. CAMADA: BIOMETRIA DE LOGS
        AnimatedVisibility(
            visible = showBiometricLogs,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.zIndex(150f)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(VelvetBlack)) {
                LogBiometricsScreen()
                TextButton(
                    onClick = { showBiometricLogs = false },
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                ) {
                    Text("VOLTAR", color = RoyalCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. CAMADA: SUCESSO DA COMPRA
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut(),
            modifier = Modifier.zIndex(200f)
        ) {
            SuccessPurchaseScreen(onGetStarted = {
                viewModel.dismissSuccessAnimation()
                forcePremiumState = true
                showPaywall = false
            })
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun HomeScreen(viewModel: MainViewModel, userIsPro: Boolean = false, onDebugPaywall: () -> Unit) {
    val context = LocalContext.current
    val isEnabled by viewModel.isServiceActive.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val blockMessage by viewModel.blockEvent.collectAsState(initial = "")
    val isFirstRun by viewModel.isFirstRun.collectAsState()
    val history by viewModel.history.collectAsState(initial = emptyList())
    val blockedCount = history.size
    val trialStart by viewModel.trialStartTimestamp.collectAsState()
// 🎯 VARIÁVEL DE CONTROLE ANTI-PISCADA
    var canShowOnboarding by remember { mutableStateOf(false) }


    var showDebugMenu by remember { mutableStateOf(false) }
    var debugClickCount by remember { mutableStateOf(0) }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.checkRoleStatus(context)
    }

    // 🎯 MATEMÁTICA REAL DE PRODUÇÃO RESTAURADA
    val currentTime = System.currentTimeMillis()
    val daysPassed = if (trialStart > 0) ((currentTime - trialStart) / (1000 * 60 * 60 * 24)).toInt() else 0
    val daysRemaining = maxOf(0, 7 - daysPassed)

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
            confirmButton = { TextButton(onClick = { showDebugMenu = false }) { Text("FECHAR") } }
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.all { it }) {
            Toast.makeText(context, "Proteção Total Ativada!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(blockMessage) {
        if (blockMessage.isNotEmpty()) snackbarHostState.showSnackbar(blockMessage, duration = SnackbarDuration.Short)
    }

    LaunchedEffect(Unit) { viewModel.checkRoleStatus(context) }

    fun hasAllPermissions(): Boolean {
        val contactPerm = ContextCompat.checkSelfPermission(context, permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val notifPerm = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        return contactPerm && notifPerm
    }

// Aguarda o Google Billing responder antes de jogar a tela na cara do usuário
    LaunchedEffect(isFirstRun, userIsPro) {
        if (isFirstRun && !userIsPro) {
            kotlinx.coroutines.delay(800) // Segura 800ms para a API do Google respirar
            if (!userIsPro) { // Se depois de 800ms ele continuar free, aí sim exibe
                canShowOnboarding = true
            }
        } else {
            canShowOnboarding = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(containerColor = Color(0xFF1E293B), contentColor = RoyalCyan, snackbarData = data)
                }
            },
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
                            if (debugClickCount >= 5) { showDebugMenu = true; debugClickCount = 0 }
                        }
                    )
                ) {
                    Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
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

                // 🛡️ PAINEL DE TELEMETRIA
                if (isEnabled) {
                    Surface(
                        color = Color(0xFF1E293B).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, RoyalCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 32.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$blockedCount",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    style = TextStyle(shadow = Shadow(color = RoyalCyan, blurRadius = 10f))
                                )
                                Text("Neutralizadas", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                            }

                            Box(modifier = Modifier.width(1.dp).height(48.dp).background(Color.White.copy(alpha = 0.1f)))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "100%",
                                        color = Color(0xFF10B981),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        style = TextStyle(shadow = Shadow(color = Color(0xFF10B981).copy(alpha = 0.6f), blurRadius = 10f))
                                    )
                                }
                                Text("Eficiência do Filtro", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }

                // 🎯 CARD DE CONTAGEM REGRESSIVA
                if (!userIsPro && trialStart > 0) {
                    TrialCountdownCard(
                        daysRemaining = daysRemaining,
                        onUpgradeClick = { onDebugPaywall() }
                    )
                }

                // BOTÃO DE AÇÃO
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
                                val perms = mutableListOf(permission.READ_CONTACTS, permission.READ_PHONE_STATE, permission.ANSWER_PHONE_CALLS)
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
                    modifier = Modifier.fillMaxWidth().height(56.dp),
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

        // --- 🚀 CAMADA DO ONBOARDING (Z-Index 50) ---
        AnimatedVisibility(
            visible = canShowOnboarding,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(50f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                WelcomeTrialSheet(onStartProtection = { viewModel.completeOnboarding() })
            }
        }
    }
}

@Composable
fun WelcomeTrialSheet(onStartProtection: () -> Unit) {
    Surface(
        color = Color(0xFF020617),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        border = BorderStroke(1.dp, RoyalCyan.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = RoyalCyan, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(24.dp))
            Text("Bem-vindo à Elite", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(
                text = "Ativamos sua Blindagem Máxima.\nAproveite 7 dias de proteção total e absoluta por conta da Blu Macaw Lab's.",
                color = Color.Gray, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 16.dp)
            )
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text("• Bloqueio Geográfico Ativo", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                Text("• Modo Stealth Habilitado", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                Text("• Zero Spam por 7 dias", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            Button(
                onClick = onStartProtection,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalCyan),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("INICIAR PROTEÇÃO", color = Color(0xFF020617), fontWeight = FontWeight.Black)
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

    Column(modifier = Modifier.fillMaxSize().background(VelvetBlack).padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Registro de Atividades", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${history.size} chamadas barradas", color = Color.Gray, fontSize = 14.sp)
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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = history, key = { it.id }) { log ->
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

    selectedLog?.let { log ->
        DecisionDialog(
            log = log,
            onDismiss = { selectedLog = null },
            onAllow = { viewModel.allowFromHistory(log); selectedLog = null },
            onBlock = { viewModel.blockFromHistory(log); selectedLog = null }
        )
    }
}

@Composable
fun HistoryItem(log: BlockedCallLog, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFF991B1B).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.number, color = Color.White, fontWeight = FontWeight.Bold)
                Text(log.blockReason, color = RoyalCyan, fontSize = 12.sp)
            }
            Text(text = dateFormat.format(Date(log.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

fun formatTime(timestamp: Long): String = DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

@Composable
fun EmptyStateHistory() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.History, contentDescription = null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("Nenhuma ameaça detectada.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            Text("Seu escudo está ativo e vigilante.", style = MaterialTheme.typography.bodySmall, color = Color.Gray.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun DecisionDialog(log: BlockedCallLog, onDismiss: () -> Unit, onAllow: () -> Unit, onBlock: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = { Text(log.number, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
        text = { Text("Este número foi bloqueado anteriormente por: ${log.blockReason}.\nO que deseja fazer?", color = Color.Gray) },
        confirmButton = { TextButton(onClick = onAllow) { Text("CONFIAR", color = Color.Green, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onBlock) { Text("MANTER BLOQUEIO", color = Color(0xFFEF4444)) } },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun TrialCountdownCard(daysRemaining: Int, onUpgradeClick: () -> Unit) {
    val accentColor = if (daysRemaining <= 2) Color(0xFFFACC15) else RoyalCyan
    val surfaceDark = Color(0xFF1E293B)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val animationSpeed = if (daysRemaining <= 2) 500 else 1000

    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = animationSpeed, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clickable { onUpgradeClick() },
        color = surfaceDark.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(accentColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (daysRemaining <= 0) Icons.Default.WorkspacePremium else Icons.Default.Timer,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp).alpha(alphaAnim)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                val titleText = if (daysRemaining <= 0) "Últimas horas de teste!" else "$daysRemaining dias de teste restantes"
                Text(titleText, color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Garanta o silêncio definitivo. A licença Vitalícia é a escolha inteligente para sua paz.",
                    color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}