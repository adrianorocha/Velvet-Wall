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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import blu.macaw.velvetwall.ui.MainViewModel
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isEnabled by viewModel.isServiceActive.collectAsState()

    // 1. Lançador para Permissões (Contatos, Notificação, etc)
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val contacts = permissions[Manifest.permission.READ_CONTACTS] ?: false
        val notifs = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false

        if (contacts || notifs) {
            Toast.makeText(context, "Permissões atualizadas!", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Lançador para Definir como App de Spam
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.checkRoleStatus(context)
    }

    // Verifica se tem todas as permissões críticas
    fun hasAllPermissions(): Boolean {
        val contactPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val notifPerm = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        return contactPerm && notifPerm
    }

    LaunchedEffect(Unit) {
        viewModel.checkRoleStatus(context)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(VelvetBlack),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícone Animado (Troca de cor se estiver ativo)
        Icon(
            imageVector = if (isEnabled) Icons.Default.Shield else Icons.Default.GppBad,
            contentDescription = null,
            tint = if (isEnabled) RoyalCyan else Color.Gray,
            modifier = Modifier.size(120.dp)
        )

        Spacer(Modifier.height(32.dp))

        Text(
            if (isEnabled) "VELVET WALL ATIVO" else "PROTEÇÃO DESATIVADA",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            if (isEnabled) "Seu dispositivo está protegido contra spam." else "Ative as permissões para bloquear chamadas.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
        )

        // BOTÃO INTELIGENTE
        Button(
            onClick = {
                if (!isEnabled) {
                    // Cenario 1: O app NÃO é padrão de spam. Pede isso primeiro.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val roleManager = context.getSystemService(RoleManager::class.java)
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        roleLauncher.launch(intent)
                    }
                } else if (!hasAllPermissions()) {
                    // Cenario 2: É padrão, mas falta permissão de contatos/notificação
                    permissionsLauncher.launch(arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ANSWER_PHONE_CALLS,
                        Manifest.permission.POST_NOTIFICATIONS
                    ))
                } else {
                    // Cenario 3: TUDO OK. Abre as configurações do Android.
                    Toast.makeText(context, "Tudo ativo! Abrindo detalhes...", Toast.LENGTH_SHORT).show()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled) VelvetBlack else RoyalCyan
            ),
            border = if (isEnabled) androidx.compose.foundation.BorderStroke(1.dp, RoyalCyan) else null,
            modifier = Modifier.fillMaxWidth(0.7f).height(50.dp)
        ) {
            if (isEnabled) {
                // Se já estiver ativo, mostra opção de revisar no sistema
                Icon(Icons.Default.CheckCircle, null, tint = RoyalCyan)
                Spacer(Modifier.width(8.dp))
                Text("Verificar Permissões", color = RoyalCyan)
            } else {
                Text("Ativar Proteção Total", color = VelvetBlack)
            }
        }
    }
}