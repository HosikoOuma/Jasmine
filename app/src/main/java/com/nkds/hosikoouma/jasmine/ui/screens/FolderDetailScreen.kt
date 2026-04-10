package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

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
    val folders by trackViewModel.folders.collectAsState()
    val folder = folders.find { it.path == folderPath }
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val haptic = LocalHapticFeedback.current
    val isInSelectionMode = selectedTracks.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        if (folder == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Folder not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(folder.tracks) { index, track ->
                    val isSelected = selectedTracks.contains(track)
                    SwipeableTrackCard(
                        track = track,
                        isCurrent = currentTrack?.id == track.id,
                        isPlaying = isPlaying,
                        isSelected = isSelected,
                        enabled = !isInSelectionMode,
                        onSwipeAction = { playerViewModel.addToQueue(track, showToast = true) },
                        onClick = {
                            if (isInSelectionMode) {
                                onToggleTrackSelection(track)
                            } else {
                                playerViewModel.playTracks(folder.tracks, index)
                                onNavigateToPlayer()
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleTrackSelection(track)
                        }
                    )
                }
            }
        }
    }
}
