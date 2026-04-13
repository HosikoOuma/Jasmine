package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nkds.hosikoouma.jasmine.core.utils.VibrationUtils
import com.nkds.hosikoouma.jasmine.datamodels.Album
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

// --- UI State ---
data class AlbumDetailUiState(
    val album: Album? = null,
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val selectedTracks: Set<Track> = emptySet()
)

// --- Stateful Screen ---
@Composable
fun AlbumDetailScreen(
    albumName: String,
    navController: NavController,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    selectedTracks: Set<Track>,
    onToggleTrackSelection: (Track) -> Unit
) {
    val albums by trackViewModel.albums.collectAsStateWithLifecycle()
    val album = remember(albums, albumName) { albums.find { it.name == albumName } }
    
    val currentTrack by playerViewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()

    val uiState = AlbumDetailUiState(
        album = album,
        currentTrack = currentTrack,
        isPlaying = isPlaying,
        selectedTracks = selectedTracks
    )

    AlbumDetailContent(
        uiState = uiState,
        onTrackClick = { index ->
            album?.let {
                if (selectedTracks.isNotEmpty()) {
                    onToggleTrackSelection(it.tracks[index])
                } else {
                    playerViewModel.playTracks(it.tracks, index)
                    onNavigateToPlayer()
                }
            }
        },
        onTrackLongClick = onToggleTrackSelection,
        onSwipeAction = { track -> playerViewModel.addToQueue(track, showToast = true) },
        onToggleTrackSelection = onToggleTrackSelection // Добавлено!
    )
}

// --- Stateless Content ---
@Composable
fun AlbumDetailContent(
    uiState: AlbumDetailUiState,
    onTrackClick: (Int) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onSwipeAction: (Track) -> Unit,
    onToggleTrackSelection: (Track) -> Unit // Добавлено!
) {
    val haptic = LocalHapticFeedback.current
    val isInSelectionMode = uiState.selectedTracks.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.album == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Album not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    AlbumHeader(uiState.album)
                }

                itemsIndexed(uiState.album.tracks, key = { _, track -> track.id }) { index, track ->
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

@Composable
private fun AlbumHeader(album: Album) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(
            albumArtUri = album.tracks.firstOrNull()?.albumArtUri,
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(20.dp)
        )
        Column {
            Text(text = album.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = album.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "${album.tracks.size} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlbumDetailPreview() {
    val sampleTrack = Track(1, "Moonlight", "Jasmine", "Garden", 200000, android.net.Uri.EMPTY, null)
    JasmineTheme {
        AlbumDetailContent(
            uiState = AlbumDetailUiState(
                album = Album("Garden", "Jasmine", listOf(sampleTrack, sampleTrack.copy(id = 2)))
            ),
            onTrackClick = {},
            onTrackLongClick = {},
            onSwipeAction = {},
            onToggleTrackSelection = {}
        )
    }
}
