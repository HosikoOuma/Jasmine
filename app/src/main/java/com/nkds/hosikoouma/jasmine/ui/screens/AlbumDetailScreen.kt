package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

@Composable
fun AlbumDetailScreen(
    albumName: String,
    navController: NavController,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit
) {
    val albums by trackViewModel.albums.collectAsState()
    val album = albums.find { it.name == albumName }
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (album == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Album not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AlbumArt(
                            albumArtUri = album.tracks.firstOrNull()?.albumArtUri,
                            modifier = Modifier.size(120.dp)
                        )
                        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.height(120.dp)) {
                            Text(album.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(album.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${album.tracks.size} tracks", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                itemsIndexed(album.tracks) { index, track ->
                    SwipeableTrackCard(
                        track = track,
                        isCurrent = currentTrack?.id == track.id,
                        isPlaying = isPlaying,
                        isSelected = false,
                        onSwipeAction = { playerViewModel.addToQueue(track, showToast = true) },
                        onClick = {
                            playerViewModel.playTracks(album.tracks, index)
                            onNavigateToPlayer()
                        }
                    )
                }
            }
        }
    }
}
