package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import kotlin.math.roundToLong

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val isCrossfadeEnabled by viewModel.isCrossfadeEnabled.collectAsState()
    val crossfadeDuration by viewModel.crossfadeDuration.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Playback",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Crossfade") },
            supportingContent = { Text("Smoothly transition between tracks") },
            trailingContent = {
                Switch(
                    checked = isCrossfadeEnabled,
                    onCheckedChange = { viewModel.setCrossfadeEnabled(it) }
                )
            }
        )

        if (isCrossfadeEnabled) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Duration", style = MaterialTheme.typography.bodyMedium)
                    Text("${(crossfadeDuration / 1000f)}s", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = crossfadeDuration.toFloat(),
                    onValueChange = { viewModel.setCrossfadeDuration(it.roundToLong()) },
                    valueRange = 1000f..10000f,
                    steps = 8
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        ListItem(
            headlineContent = { Text("Version") },
            supportingContent = { Text("1.0.0 (Jasmine)") }
        )
    }
}
