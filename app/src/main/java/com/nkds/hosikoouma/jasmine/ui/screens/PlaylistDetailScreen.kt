package com.nkds.hosikoouma.jasmine.ui.screens

import android.net.Uri
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.core.utils.VibrationUtils
import com.nkds.hosikoouma.jasmine.data.PlaylistEntity
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.ui.components.vibrateClick
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

// --- UI State ---
data class PlaylistDetailUiState(
    val playlist: PlaylistEntity? = null,
    val tracks: List<Track> = emptyList(),
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val selectedTracks: Set<Track> = emptySet()
)

// --- Stateful Screen ---
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
    val playlists by trackViewModel.playlists.collectAsStateWithLifecycle()
    val playlist = remember(playlists, playlistId) { 
        playlists.find { it.id == playlistId }?.let { PlaylistEntity(it.id, it.name, it.coverUri?.toString(), it.createdAt) }
    }
    val playlistTracks by trackViewModel.getTracksForPlaylist(playlistId).collectAsStateWithLifecycle(initialValue = emptyList())
    
    val currentTrack by playerViewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()

    val uiState = PlaylistDetailUiState(
        playlist = playlist,
        tracks = playlistTracks,
        currentTrack = currentTrack,
        isPlaying = isPlaying,
        selectedTracks = selectedTracks
    )

    PlaylistDetailContent(
        uiState = uiState,
        onAddTracksClick = onAddTracksClick,
        onTrackClick = { index ->
            if (selectedTracks.isNotEmpty()) {
                onToggleTrackSelection(playlistTracks[index])
            } else {
                playerViewModel.playTracks(playlistTracks, index)
                onNavigateToPlayer()
            }
        },
        onTrackLongClick = onToggleTrackSelection,
        onSwipeAction = { track -> playerViewModel.addToQueue(track, showToast = true) },
        onShufflePlay = {
            if (playlistTracks.isNotEmpty()) {
                playerViewModel.shuffleAndPlay(playlistTracks)
                onNavigateToPlayer()
            }
        }
    )
}

// --- Stateless Content ---
@Composable
fun PlaylistDetailContent(
    uiState: PlaylistDetailUiState,
    onAddTracksClick: () -> Unit,
    onTrackClick: (Int) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onSwipeAction: (Track) -> Unit,
    onShufflePlay: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val isInSelectionMode = uiState.selectedTracks.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.playlist == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Playlist not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    PlaylistHeader(
                        playlist = uiState.playlist,
                        tracksCount = uiState.tracks.size,
                        firstTrackArt = uiState.tracks.firstOrNull()?.albumArtUri,
                        onShuffleClick = {
                            vibrateClick(context)
                            onShufflePlay()
                        },
                        showShuffle = !isInSelectionMode && uiState.tracks.isNotEmpty()
                    )
                }

                if (uiState.tracks.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxHeight(0.6f)) {
                            EmptyPlaylistPlaceholder(onAddTracksClick)
                        }
                    }
                } else {
                    itemsIndexed(
                        items = uiState.tracks,
                        key = { _, track -> track.id }
                    ) { index, track ->
                        val isSelected = uiState.selectedTracks.contains(track)
                        SwipeableTrackCard(
                            track = track,
                            isCurrent = uiState.currentTrack?.id == track.id,
                            isPlaying = uiState.isPlaying,
                            isSelected = isSelected,
                            isManualMarkingEnabled = true, // Включаем метки очереди
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
}

@Composable
private fun PlaylistHeader(
    playlist: PlaylistEntity,
    tracksCount: Int,
    firstTrackArt: Uri?,
    onShuffleClick: () -> Unit,
    showShuffle: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            val displayArt = remember(playlist.coverUri, firstTrackArt) {
                playlist.coverUri?.let { Uri.parse(it) } ?: firstTrackArt
            }

            AlbumArt(
                albumArtUri = displayArt,
                modifier = Modifier
                    .size(150.dp) // Увеличена с 100dp до 130dp
                    .clip(RoundedCornerShape(24.dp)),
                isLowRes = false
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = "$tracksCount tracks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Playlist",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showShuffle) {
            ShuffleButton(
                modifier = Modifier.padding(start = 8.dp),
                onShuffle = onShuffleClick
            )
        }
    }
}

@Composable
private fun EmptyPlaylistPlaceholder(onAddTracksClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
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
}

@Preview(showBackground = true)
@Composable
fun PlaylistDetailPreview() {
    val sampleTrack = Track(1, "Moonlight", "Jasmine", "Garden", 200000, android.net.Uri.EMPTY, null)
    JasmineTheme {
        PlaylistDetailContent(
            uiState = PlaylistDetailUiState(
                playlist = PlaylistEntity(1, "Night Vibes"),
                tracks = listOf(sampleTrack, sampleTrack.copy(id = 2))
            ),
            onAddTracksClick = {},
            onTrackClick = {},
            onTrackLongClick = {},
            onSwipeAction = {},
            onShufflePlay = {}
        )
    }
}
