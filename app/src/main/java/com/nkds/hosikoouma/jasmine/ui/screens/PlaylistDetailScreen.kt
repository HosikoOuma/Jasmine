package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    navController: NavController,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    onAddTracksClick: () -> Unit,
    selectedTracks: Set<Track>,
    onToggleTrackSelection: (Track) -> Unit
) {
    val playlists by trackViewModel.playlists.collectAsState()
    val playlist = playlists.find { it.id == playlistId }
    val playlistTracks by trackViewModel.getTracksForPlaylist(playlistId).collectAsState(initial = emptyList())
    
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val haptic = LocalHapticFeedback.current
    val isInSelectionMode = selectedTracks.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        if (playlist == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Playlist not found")
            }
        } else if (playlistTracks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "It seems like there is nothing here.\nWant to fix it?",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAddTracksClick,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Tracks")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(playlistTracks) { index, track ->
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
                                playerViewModel.playTracks(playlistTracks, index)
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
