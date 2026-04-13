package com.nkds.hosikoouma.jasmine.ui.screens

import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkds.hosikoouma.jasmine.core.utils.VibrationUtils
import com.nkds.hosikoouma.jasmine.data.RadioStation
import com.nkds.hosikoouma.jasmine.ui.components.PlayingEqualizer
import com.nkds.hosikoouma.jasmine.ui.components.bouncingClickable
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.RadioViewModel

// --- UI State ---
data class RadioUiState(
    val stations: List<RadioStation> = emptyList(),
    val currentStation: RadioStation? = null,
    val isPlaying: Boolean = false,
    val isRadioMode: Boolean = false,
    val selectedStations: Set<RadioStation> = emptySet()
)

// --- Stateful Screen ---
@Composable
fun RadioScreen(
    viewModel: RadioViewModel,
    playerViewModel: PlayerViewModel,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onStationClick: (RadioStation) -> Unit,
    selectedStations: Set<RadioStation> = emptySet(),
    onToggleSelection: (RadioStation) -> Unit = {}
) {
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val currentStation by playerViewModel.currentRadioStation.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val isRadioMode by playerViewModel.isRadioMode.collectAsStateWithLifecycle()

    val uiState = RadioUiState(
        stations = stations,
        currentStation = currentStation,
        isPlaying = isPlaying,
        isRadioMode = isRadioMode,
        selectedStations = selectedStations
    )

    RadioContent(
        uiState = uiState,
        showAddDialog = showAddDialog,
        onDismissDialog = onDismissDialog,
        onStationClick = onStationClick,
        onToggleSelection = onToggleSelection,
        onAddStation = viewModel::addStation
    )
}

// --- Stateless Content ---
@Composable
fun RadioContent(
    uiState: RadioUiState,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onStationClick: (RadioStation) -> Unit,
    onToggleSelection: (RadioStation) -> Unit,
    onAddStation: (String, String) -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.stations.isEmpty()) {
            EmptyRadioPlaceholder()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.stations, key = { it.id }) { station ->
                    val isSelected = uiState.selectedStations.contains(station)
                    val isCurrent = uiState.isRadioMode && uiState.currentStation?.id == station.id
                    
                    RadioStationCard(
                        station = station,
                        isCurrent = isCurrent,
                        isPlaying = uiState.isPlaying,
                        isSelected = isSelected,
                        onClick = {
                            if (uiState.selectedStations.isNotEmpty()) {
                                VibrationUtils.selectionVibrate(vibrator)
                                onToggleSelection(station)
                            } else {
                                onStationClick(station)
                            }
                        },
                        onLongClick = { 
                            VibrationUtils.selectionVibrate(vibrator)
                            onToggleSelection(station) 
                        }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddStationDialog(
                onDismiss = onDismissDialog,
                onConfirm = onAddStation
            )
        }
    }
}

// --- Internal Components ---

@Composable
private fun EmptyRadioPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Radio,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No radio stations yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RadioStationCard(
    station: RadioStation,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.secondaryContainer
            isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(500),
        label = "cardColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingClickable(onLongClick = onLongClick, onClick = onClick)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent || isSelected) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                RadioIconBox(isCurrent)
                if (isSelected) SelectionBadge()
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isCurrent) {
                PlayingEqualizer(
                    isPlaying = isPlaying,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RadioIconBox(isCurrent: Boolean) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.primaryContainer
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Radio,
            contentDescription = null,
            tint = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun SelectionBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
        modifier = Modifier.offset(x = 4.dp, y = 4.dp).size(20.dp)
    ) {
        Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(2.dp)
        )
    }
}

@Composable
private fun AddStationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var stationName by remember { mutableStateOf("") }
    var stationUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Radio Station") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = stationName,
                    onValueChange = { stationName = it },
                    label = { Text("Station Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = stationUrl,
                    onValueChange = { stationUrl = it },
                    label = { Text("Station URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (stationName.isNotBlank() && stationUrl.isNotBlank()) {
                        onConfirm(stationName, stationUrl)
                        onDismiss()
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun RadioPreview() {
    JasmineTheme {
        RadioContent(
            uiState = RadioUiState(
                stations = listOf(
                    RadioStation(1, "Sample FM", "http://stream.url"),
                    RadioStation(2, "Jasmine Hits", "http://radio.url")
                ),
                isPlaying = true,
                isRadioMode = true,
                currentStation = RadioStation(1, "Sample FM", "http://stream.url")
            ),
            showAddDialog = false,
            onDismissDialog = {},
            onStationClick = {},
            onToggleSelection = {},
            onAddStation = { _, _ -> }
        )
    }
}
