package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nkds.hosikoouma.jasmine.R
import com.nkds.hosikoouma.jasmine.core.utils.VibrationUtils
import com.nkds.hosikoouma.jasmine.datamodels.Artist
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

// --- UI State ---
data class ArtistDetailUiState(
    val artist: Artist? = null,
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val selectedTracks: Set<Track> = emptySet()
)

// --- Stateful Screen ---
@Composable
fun ArtistDetailScreen(
    artistName: String,
    navController: NavController,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    selectedTracks: Set<Track>,
    onToggleTrackSelection: (Track) -> Unit
) {
    val artists by trackViewModel.artists.collectAsStateWithLifecycle()
    val artist = remember(artists, artistName) { artists.find { it.name == artistName } }
    
    val currentTrack by playerViewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()

    val sourceName = if (artist != null) stringResource(R.string.artist_source, artist.name) else ""

    val uiState = ArtistDetailUiState(
        artist = artist,
        currentTrack = currentTrack,
        isPlaying = isPlaying,
        selectedTracks = selectedTracks
    )

    ArtistDetailContent(
        uiState = uiState,
        onTrackClick = { index ->
            artist?.let {
                if (selectedTracks.isNotEmpty()) {
                    onToggleTrackSelection(it.tracks[index])
                } else {
                    playerViewModel.playTracks(it.tracks, index, sourceName = sourceName)
                    onNavigateToPlayer()
                }
            }
        },
        onTrackLongClick = onToggleTrackSelection,
        onSwipeAction = { track -> playerViewModel.addToQueue(track, showToast = true) }
    )
}

// --- Stateless Content ---
@Composable
fun ArtistDetailContent(
    uiState: ArtistDetailUiState,
    onTrackClick: (Int) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onSwipeAction: (Track) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isInSelectionMode = uiState.selectedTracks.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.artist == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.artist_not_found))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ArtistHeader(uiState.artist)
                }

                itemsIndexed(uiState.artist.tracks, key = { _, track -> track.id }) { index, track ->
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

// --- Internal Components ---

@Composable
private fun ArtistHeader(artist: Artist) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            artist.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.tracks_count, artist.tracks.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ArtistDetailPreview() {
    val sampleTrack = Track(1, "Moonlight", "Jasmine", "Garden", 200000, android.net.Uri.EMPTY, null)
    JasmineTheme {
        ArtistDetailContent(
            uiState = ArtistDetailUiState(
                artist = Artist("Jasmine", listOf(sampleTrack, sampleTrack.copy(id = 2)))
            ),
            onTrackClick = {},
            onTrackLongClick = {},
            onSwipeAction = {}
        )
    }
}
