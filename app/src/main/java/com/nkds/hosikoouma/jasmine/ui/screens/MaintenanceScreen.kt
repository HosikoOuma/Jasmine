package com.nkds.hosikoouma.jasmine.ui.screens

import android.text.format.Formatter
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkds.hosikoouma.jasmine.viewmodels.MaintenanceViewModel
import kotlin.math.roundToInt

@Composable
fun MaintenanceScreen(
    viewModel: MaintenanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showConfirmCoverDialog by remember { mutableStateOf(false) }
    var showConfirmTelegramDialog by remember { mutableStateOf(false) }

    val formattedCoverSize = Formatter.formatFileSize(context, state.coverSize)
    val formattedTelegramSize = Formatter.formatFileSize(context, state.telegramCacheSize)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- Storage Section ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Storage & Cache",
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
                                text = "Cover Art Cache",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${state.coverCount} files • $formattedCoverSize",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = "Covers are cached to improve scrolling performance. Clearing them will force the app to re-scan covers on the next launch.",
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
                        enabled = !state.isClearing && state.coverCount > 0
                    ) {
                        if (state.isClearing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.DeleteForever, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Clear Cover Cache")
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
                                text = "Telegram Cloud Cache",
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
                        text = "Music streamed from Telegram is cached locally for smooth playback. Clearing this will delete all downloaded cloud tracks.",
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
                        enabled = !state.isClearing && state.telegramCacheSize > 0
                    ) {
                        if (state.isClearing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.DeleteForever, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Clear Telegram Cache")
                        }
                    }
                }
            }
        }

        // --- Statistics Section ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Statistics & Recommendations",
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
                        Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "On Repeat Interval",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Set the number of days to analyze for the \"On Repeat\" playlist. Currently set to ${state.onRepeatInterval} days.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    Slider(
                        value = state.onRepeatInterval.toFloat(),
                        onValueChange = { viewModel.setOnRepeatInterval(it.roundToInt()) },
                        valueRange = 1f..30f,
                        steps = 29,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1 day", style = MaterialTheme.typography.labelSmall)
                        Text("30 days", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(Modifier.height(24.dp))

                    // Force Refresh On Repeat
                    Button(
                        onClick = { viewModel.forceRefreshOnRepeat() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        enabled = !state.isRefreshingOnRepeat
                    ) {
                        if (state.isRefreshingOnRepeat) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh \"On Repeat\" Now")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(160.dp))
    }

    if (showConfirmCoverDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCoverDialog = false },
            title = { Text("Clear cache?") },
            text = { Text("This will delete all cached album covers. They will be regenerated the next time you scan your library.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmCoverDialog = false
                        viewModel.clearCoverCache()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCoverDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConfirmTelegramDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmTelegramDialog = false },
            title = { Text("Clear Telegram cache?") },
            text = { Text("This will delete all downloaded audio files from Telegram. Your account and channels will remain connected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmTelegramDialog = false
                        viewModel.clearTelegramCache()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmTelegramDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
