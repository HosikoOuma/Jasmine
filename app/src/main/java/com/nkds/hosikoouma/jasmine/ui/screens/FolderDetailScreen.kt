package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nkds.hosikoouma.jasmine.core.utils.VibrationUtils
import com.nkds.hosikoouma.jasmine.datamodels.Folder
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

// --- UI State ---
data class FolderDetailUiState(
    val folder: Folder? = null,
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val selectedTracks: Set<Track> = emptySet()
)

// --- Stateful Screen ---
@Composable
fun FolderDetailScreen(
    folderPath: String,
    navController: NavController,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    selectedTracks: Set<Track>,
    onToggleTrackSelection: (Track) -> Unit
) {
    val folders by trackViewModel.folders.collectAsStateWithLifecycle()
    val folder = remember(folders, folderPath) { folders.find { it.path == folderPath } }
    
    val currentTrack by playerViewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()

    val uiState = FolderDetailUiState(
        folder = folder,
        currentTrack = currentTrack,
        isPlaying = isPlaying,
        selectedTracks = selectedTracks
    )

    FolderDetailContent(
        uiState = uiState,
        onTrackClick = { index ->
            folder?.let {
                if (selectedTracks.isNotEmpty()) {
                    onToggleTrackSelection(it.tracks[index])
                } else {
                    playerViewModel.playTracks(it.tracks, index, sourceName = "Folder: ${it.name}")
                    onNavigateToPlayer()
                }
            }
        },
        onTrackLongClick = onToggleTrackSelection,
        onSwipeAction = { track -> playerViewModel.addToQueue(track, showToast = true) },
        onSelectAll = {
            folder?.tracks?.forEach { track ->
                if (!selectedTracks.contains(track)) onToggleTrackSelection(track)
            }
        }
    )
}

// --- Stateless Content ---
@Composable
fun FolderDetailContent(
    uiState: FolderDetailUiState,
    onTrackClick: (Int) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onSwipeAction: (Track) -> Unit,
    onSelectAll: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val isInSelectionMode = uiState.selectedTracks.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.folder == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Folder not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(uiState.folder.tracks, key = { _, track -> track.id }) { index, track ->
                    val isSelected = uiState.selectedTracks.contains(track)
                    SwipeableTrackCard(
                        track = track,
                        isCurrent = uiState.currentTrack?.id == track.id,
                        isPlaying = uiState.isPlaying,
                        isSelected = isSelected,
                        enabled = !isInSelectionMode,
                        onSwipeAction = { onSwipeAction(track) },
                        onClick = { onTrackClick(index) },
                        onLongClick = {
                            VibrationUtils.performLongPressHaptic(haptic)
                            onTrackLongClick(track)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FolderDetailPreview() {
    val sampleTrack = Track(1, "Deep Forest", "Nature", "Ambient", 300000, android.net.Uri.EMPTY, null)
    JasmineTheme {
        FolderDetailContent(
            uiState = FolderDetailUiState(
                folder = Folder("Ambient", "/music/ambient", listOf(sampleTrack, sampleTrack.copy(id = 2)))
            ),
            onTrackClick = {},
            onTrackLongClick = {},
            onSwipeAction = {}
        )
    }
}
