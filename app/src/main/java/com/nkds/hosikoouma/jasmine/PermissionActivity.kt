package com.nkds.hosikoouma.jasmine

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import kotlinx.coroutines.delay

class PermissionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (hasRequiredPermissions()) {
            startMainActivity()
            return
        }

        enableEdgeToEdge()
        setContent {
            JasmineTheme {
                PermissionScreen(
                    onPermissionsGranted = { startMainActivity() },
                    onRequestManageStorage = { requestManageExternalStorage() }
                )
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        val mediaGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else true

        return storageGranted && mediaGranted
    }

    private fun requestManageExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${packageName}")
            }
            startActivity(intent)
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (hasRequiredPermissions()) {
            startMainActivity()
        }
    }
}

@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit, onRequestManageStorage: () -> Unit) {
    var showWarning by remember { mutableStateOf(true) }
    var secondsLeft by remember { mutableIntStateOf(5) }

    // Таймер для предупреждения
    LaunchedEffect(showWarning) {
        if (showWarning) {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
        }
    }

    val permissionsToRequest = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                onRequestManageStorage()
            } else if (permissions.values.all { it }) {
                onPermissionsGranted()
            }
        } else if (permissions.values.all { it }) {
            onPermissionsGranted()
        }
    }

    if (showWarning) {
        AlertDialog(
            onDismissRequest = { /* Не позволяем закрыть тапом вне */ },
            icon = { Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("ПРИЛОЖЕНИЕ\nНЕСТАБИЛЬНО") },
            text = {
                Text(
                    "Приложение навайбкожено через Gemini и сделано для личного пользования. Оно нестабильно и нуждается в проверке, потому Jasmine может выкинуть баги, лаги и прочие невкусные приколы.\n\nО проблемах писать @NekoDosi.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showWarning = false },
                    enabled = secondsLeft == 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(if (secondsLeft > 0) "Подожди $secondsLeft..." else "Смириться")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Jasmine needs full access to storage to manage your playlists and covers directly in the Music folder.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { launcher.launch(permissionsToRequest) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant All Access")
            }
        }
    }
}
