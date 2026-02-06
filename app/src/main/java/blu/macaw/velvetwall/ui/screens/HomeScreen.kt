package blu.macaw.velvetwall.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import blu.macaw.velvetwall.ui.MainViewModel
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isEnabled by viewModel.isServiceActive.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val blockMessage by viewModel.blockEvent.collectAsState(initial = "")

    // --- LANÇADORES DE PERMISSÃO ---

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkRoleStatus(context)
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(context, "Todas as proteções ativadas!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- LÓGICA DE APOIO ---

    fun hasAllPermissions(): Boolean {
        val contactPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val notifPerm = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        return contactPerm && notifPerm
    }

    // Monitora bloqueios em tempo real para disparar a animação (Snackbar)
    LaunchedEffect(blockMessage) {
        viewModel.blockEvent.collect { message ->
            if (message.isNotEmpty()) {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkRoleStatus(context)
    }

    // --- INTERFACE (UI) ---

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    containerColor = Color(0xFF1E293B), // Velvet Dark
                    contentColor = RoyalCyan,
                    snackbarData = data
                )
            }
        },
        containerColor = VelvetBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícone do Escudo com Branding Blu Macaw Lab's
            Box(contentAlignment = Alignment.Center) {
                if (isEnabled) {
                    // Círculo de brilho suave atrás do escudo quando ativo
                    Surface(
                        modifier = Modifier.size(160.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = RoyalCyan.copy(alpha = 0.1f)
                    ) {}
                }

                Icon(
                    imageVector = if (isEnabled) Icons.Default.Shield else Icons.Default.GppBad,
                    contentDescription = "Status de Proteção",
                    tint = if (isEnabled) RoyalCyan else Color.Gray,
                    modifier = Modifier.size(120.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = if (isEnabled) "VELVET WALL ATIVO" else "PROTEÇÃO DESATIVADA",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isEnabled)
                    "Sua privacidade está blindada pela Blu Macaw."
                else
                    "Conceda as permissões necessárias para iniciar o bloqueio inteligente.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 48.dp)
            )

            // BOTÃO DE AÇÃO PREMIUM
            Button(
                onClick = {
                    val roleManager = context.getSystemService(RoleManager::class.java)

                    when {
                        // 1. O app ainda não é o gerenciador de chamadas padrão
                        roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                roleLauncher.launch(intent)
                            }
                        }

                        // 2. É padrão, mas faltam as permissões de execução (Contatos/Notif)
                        !hasAllPermissions() -> {
                            val perms = mutableListOf(
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.ANSWER_PHONE_CALLS
                            )
                            if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)

                            permissionsLauncher.launch(perms.toTypedArray())
                        }

                        // 3. Tudo configurado: Oferece gerenciar no sistema
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) Color.Transparent else RoyalCyan
                ),
                border = if (isEnabled) BorderStroke(1.dp, RoyalCyan) else null
            ) {
                if (isEnabled) {
                    Icon(Icons.Default.CheckCircle, null, tint = RoyalCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("REVISAR PROTEÇÃO", color = RoyalCyan, fontWeight = FontWeight.Bold)
                } else {
                    Text("ATIVAR ESCUDO VELVET", color = VelvetBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}