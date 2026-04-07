package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val isCrossfadeEnabled by viewModel.isCrossfadeEnabled.collectAsState()
    val crossfadeDuration by viewModel.crossfadeDuration.collectAsState()
    val minTrackDuration by viewModel.minTrackDuration.collectAsState()
    val defaultSortType by viewModel.defaultSortType.collectAsState()
    val isDefaultSortReversed by viewModel.isDefaultSortReversed.collectAsState()

    var showSortDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- PLAYBACK ---
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
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- LIBRARY ---
        Text(
            text = "Library",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Filter short tracks") },
            supportingContent = { Text("Hide tracks shorter than ${minTrackDuration}s") }
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Slider(
                value = minTrackDuration.toFloat(),
                onValueChange = { viewModel.setMinTrackDuration(it.roundToInt()) },
                valueRange = 0f..30f,
                steps = 29
            )
        }

        // --- DEFAULT SORTING ---
        ListItem(
            headlineContent = { Text("Default Sorting") },
            supportingContent = { 
                val sortLabel = when(defaultSortType) {
                    "BY_NAME" -> "Name"
                    "BY_ARTIST" -> "Artist"
                    "BY_DURATION" -> "Duration"
                    else -> "Date Added"
                }
                Text("Currently sorted by $sortLabel ${if(isDefaultSortReversed) "(Reversed)" else ""}")
            },
            modifier = Modifier.clickable { showSortDialog = true }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
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

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Default Sorting") },
            text = {
                Column {
                    SortOption("By Name", "BY_NAME", defaultSortType) { viewModel.setDefaultSortType(it) }
                    SortOption("By Artist", "BY_ARTIST", defaultSortType) { viewModel.setDefaultSortType(it) }
                    SortOption("By Date Added", "BY_DATE", defaultSortType) { viewModel.setDefaultSortType(it) }
                    SortOption("By Duration", "BY_DURATION", defaultSortType) { viewModel.setDefaultSortType(it) }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setDefaultSortReversed(!isDefaultSortReversed) }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(checked = isDefaultSortReversed, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reverse order by default")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
fun SortOption(
    label: String,
    value: String,
    currentValue: String,
    onSelect: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(value) }
            .padding(vertical = 8.dp)
    ) {
        RadioButton(selected = value == currentValue, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}
