package com.nkds.hosikoouma.jasmine.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkds.hosikoouma.jasmine.R
import com.nkds.hosikoouma.jasmine.ui.components.SettingsClickableItem
import com.nkds.hosikoouma.jasmine.viewmodels.MaintenanceViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import android.content.Intent
import android.net.Uri

@Composable
fun MaintenanceScreen(
    maintenanceViewModel: MaintenanceViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val state by maintenanceViewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showConfirmCoverDialog by remember { mutableStateOf(false) }
    var showConfirmTelegramDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val formattedCoverSize = Formatter.formatFileSize(context, state.coverSize)
    val formattedTelegramSize = Formatter.formatFileSize(context, state.telegramCacheSize)

    val languages = listOf(
        "default" to stringResource(R.string.system_default),
        "en" to "English",
        "ru" to "Русский"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- Updates Section ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.updates),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Update, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (state.latestRelease != null) {
                                    stringResource(R.string.update_available)
                                } else {
                                    stringResource(R.string.check_for_updates)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (state.latestRelease != null) {
                                    stringResource(R.string.version_info, state.currentVersion, state.latestRelease?.tagName ?: "")
                                } else {
                                    stringResource(R.string.version_template, state.currentVersion)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (state.latestRelease != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = state.latestRelease?.body ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (state.latestRelease != null) {
                        val apkAsset = state.latestRelease?.assets?.find { it.name.endsWith(".apk") }
                        Button(
                            onClick = {
                                val url = apkAsset?.downloadUrl ?: state.latestRelease?.htmlUrl
                                url?.let {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Rounded.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.download_update))
                        }
                    } else {
                        Button(
                            onClick = { maintenanceViewModel.checkForUpdates() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isCheckingForUpdates
                        ) {
                            if (state.isCheckingForUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Icon(Icons.Rounded.Refresh, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.check_for_updates))
                            }
                        }
                    }
                    
                    state.updateError?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (error == "UP_TO_DATE") {
                                stringResource(R.string.up_to_date)
                            } else {
                                stringResource(R.string.failed_to_check_update)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (error == "UP_TO_DATE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // --- Language Section ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.app_language),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            SettingsClickableItem(
                title = stringResource(R.string.app_language),
                subtitle = languages.find { it.first == settings.language }?.second ?: settings.language,
                icon = Icons.Rounded.Language,
                onClick = { showLanguageDialog = true }
            )
            
            Text(
                text = stringResource(R.string.language_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // --- Storage Section ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.storage_cache),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Cover Art Cache Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.cover_cache),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.cover_cache_info, state.coverCount, formattedCoverSize),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.cover_cache_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { showConfirmCoverDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        enabled = !state.isClearingCovers && state.coverCount > 0
                    ) {
                        if (state.isClearingCovers) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onErrorContainer)
                        } else {
                            Icon(Icons.Rounded.DeleteForever, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.clear_cover_cache))
                        }
                    }
                }
            }

            // Telegram Cloud Cache Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.telegram_cache),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formattedTelegramSize,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.telegram_cache_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { showConfirmTelegramDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        enabled = !state.isClearingTelegram && state.telegramCacheSize > 0
                    ) {
                        if (state.isClearingTelegram) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onErrorContainer)
                        } else {
                            Icon(Icons.Rounded.DeleteForever, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.clear_telegram_cache))
                        }
                    }
                }
            }
        }

        // --- Debug Section ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.debug_development),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.BugReport, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.trigger_app_crash),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.trigger_app_crash_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { maintenanceViewModel.triggerCrash() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Rounded.Warning, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.simulate_crash))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(160.dp))
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.app_language)) },
            text = {
                Column {
                    languages.forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = settings.language == code, onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showConfirmCoverDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCoverDialog = false },
            title = { Text(stringResource(R.string.clear_cache_confirm_title)) },
            text = { Text(stringResource(R.string.clear_cover_cache_confirm_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmCoverDialog = false
                        maintenanceViewModel.clearCoverCache()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCoverDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showConfirmTelegramDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmTelegramDialog = false },
            title = { Text(stringResource(R.string.clear_telegram_cache_confirm_title)) },
            text = { Text(stringResource(R.string.clear_telegram_cache_confirm_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmTelegramDialog = false
                        maintenanceViewModel.clearTelegramCache()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmTelegramDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
