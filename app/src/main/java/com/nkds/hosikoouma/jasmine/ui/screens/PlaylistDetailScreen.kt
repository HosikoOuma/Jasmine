package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    navController: NavController,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit
) {
    val playlists by trackViewModel.playlists.collectAsState()
    val playlist = playlists.find { it.id == playlistId }
    val playlistTracks by trackViewModel.getTracksForPlaylist(playlistId).collectAsState(initial = emptyList())
    
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    var showTrackPicker by remember { mutableStateOf(false) }

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
                    onClick = { showTrackPicker = true },
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
                    SwipeableTrackCard(
                        track = track,
                        isCurrent = currentTrack?.id == track.id,
                        isPlaying = isPlaying,
                        isSelected = false,
                        onSwipeAction = { playerViewModel.addToQueue(track, showToast = true) },
                        onClick = {
                            playerViewModel.playTracks(playlistTracks, index)
                            onNavigateToPlayer()
                        }
                    )
                }
            }
        }
    }

    if (showTrackPicker) {
        val allTracks by trackViewModel.allTracks.collectAsState()
        
        AlertDialog(
            onDismissRequest = { showTrackPicker = false },
            title = { Text("Select Tracks") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    itemsIndexed(allTracks) { _, track ->
                        val isAlreadyInPlaylist = playlistTracks.any { it.id == track.id }
                        ListItem(
                            headlineContent = { 
                                Text(
                                    track.title, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAlreadyInPlaylist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            supportingContent = { Text(track.artist) },
                            trailingContent = {
                                if (isAlreadyInPlaylist) {
                                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                }
                            },
                            modifier = Modifier.clickable {
                                trackViewModel.addTrackToPlaylist(playlistId, track.id)
                                // We keep the picker open to add multiple tracks
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrackPicker = false }) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
