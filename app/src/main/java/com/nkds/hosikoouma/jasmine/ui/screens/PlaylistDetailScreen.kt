package com.nkds.hosikoouma.jasmine.ui.screens

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
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
import com.nkds.hosikoouma.jasmine.ui.components.simpleVerticalScrollbar
import com.nkds.hosikoouma.jasmine.ui.components.vibrateClick
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
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
    val playlists by trackViewModel.playlists.collectAsStateWithLifecycle()
    val playlist = remember(playlists, playlistId) { 
        playlists.find { it.id == playlistId }?.let { PlaylistEntity(it.id, it.name, it.coverUri?.toString(), it.createdAt) }
    }
    val playlistTracks by trackViewModel.getTracksForPlaylist(playlistId).collectAsStateWithLifecycle(initialValue = emptyList())
    
    val currentTrack by playerViewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val searchQuery by trackViewModel.searchQuery.collectAsStateWithLifecycle()

    val filteredTracks = remember(playlistTracks, searchQuery) {
        playlistTracks.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    PlaylistDetailContent(
        uiState = PlaylistDetailUiState(playlist, playlistTracks, currentTrack, isPlaying, selectedTracks),
        filteredTracks = filteredTracks,
        onAddTracksClick = onAddTracksClick,
        onTrackClick = { track ->
            if (selectedTracks.isNotEmpty()) {
                onToggleTrackSelection(track)
            } else {
                val originalIndex = playlistTracks.indexOf(track)
                playerViewModel.playTracks(playlistTracks, originalIndex, sourceName = "Playlist: ${playlist?.name}")
                onNavigateToPlayer()
            }
        },
        onTrackLongClick = onToggleTrackSelection,
        onSwipeAction = { track -> playerViewModel.addToQueue(track, showToast = true) },
        onShufflePlay = {
            if (playlistTracks.isNotEmpty()) {
                playerViewModel.shuffleAndPlay(playlistTracks, sourceName = "Playlist: ${playlist?.name}")
                onNavigateToPlayer()
            }
        }
    )
}

@Composable
fun PlaylistDetailContent(
    uiState: PlaylistDetailUiState,
    filteredTracks: List<Track>,
    onAddTracksClick: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onSwipeAction: (Track) -> Unit,
    onShufflePlay: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.playlist == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Playlist not found")
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().simpleVerticalScrollbar(listState),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    PlaylistHeader(
                        playlist = uiState.playlist,
                        tracksCount = uiState.tracks.size,
                        firstTrackArt = uiState.tracks.firstOrNull()?.albumArtUri,
                        onShuffleClick = { vibrateClick(context); onShufflePlay() },
                        showShuffle = uiState.selectedTracks.isEmpty() && uiState.tracks.isNotEmpty()
                    )
                }

                if (uiState.tracks.isEmpty()) {
                    item { Box(modifier = Modifier.fillParentMaxHeight(0.6f)) { EmptyPlaylistPlaceholder(onAddTracksClick) } }
                } else {
                    itemsIndexed(filteredTracks, key = { _, track -> track.id }) { _, track ->
                        SwipeableTrackCard(
                            track = track,
                            isCurrent = uiState.currentTrack?.id == track.id,
                            isPlaying = uiState.isPlaying,
                            isSelected = uiState.selectedTracks.contains(track),
                            isManualMarkingEnabled = true,
                            enabled = uiState.selectedTracks.isEmpty(),
                            onSwipeAction = { onSwipeAction(track) },
                            onClick = { onTrackClick(track) },
                            onLongClick = { VibrationUtils.performLongPressHaptic(haptic); onTrackLongClick(track) }
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
    var isExpanded by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "expandProgress"
    )

    val artSize = (150.dp + (screenWidth - 32.dp - 150.dp) * expandProgress).coerceAtLeast(0.dp)
    
    // Вместо использования offsets, которые заставляют элементы "летать" над другими,
    // используем Column, где элементы просто меняют свои размеры и веса.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .animateContentSize(animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Контейнер, который может быть либо Row (когда свернуто), либо Column (когда развернуто)
        // Но чтобы анимация была плавной, мы всегда используем одну структуру.
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Обложка
            PlaylistArt(
                playlist = playlist,
                firstTrackArt = firstTrackArt,
                size = artSize,
                onClick = { isExpanded = !isExpanded }
            )

            // Если не расширено, показываем текст справа
            if (expandProgress < 0.5f) {
                Spacer(modifier = Modifier.width((20.dp * (1f - expandProgress * 2)).coerceAtLeast(0.dp)))
                
                PlaylistInfo(
                    tracksCount = tracksCount,
                    modifier = Modifier.weight(1f).graphicsLayer { alpha = 1f - expandProgress * 2 }
                )

                if (showShuffle) {
                    ShuffleButton(
                        modifier = Modifier.padding(start = 8.dp).graphicsLayer { alpha = 1f - expandProgress * 2 },
                        onShuffle = onShuffleClick
                    )
                }
            }
        }

        // Если расширено, показываем текст снизу
        if (expandProgress >= 0.5f) {
            Spacer(modifier = Modifier.height((16.dp * ((expandProgress - 0.5f) * 2)).coerceAtLeast(0.dp)))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .graphicsLayer { alpha = (expandProgress - 0.5f) * 2 },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlaylistInfo(tracksCount = tracksCount, modifier = Modifier.weight(1f))
                
                if (showShuffle) {
                    ShuffleButton(
                        modifier = Modifier,
                        onShuffle = onShuffleClick
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistArt(
    playlist: PlaylistEntity,
    firstTrackArt: Uri?,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayArt = remember(playlist.coverUri, firstTrackArt) {
        playlist.coverUri?.let { Uri.parse(it) } ?: firstTrackArt
    }

    AlbumArt(
        albumArtUri = displayArt,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        isLowRes = false
    )
}

@Composable
private fun PlaylistInfo(tracksCount: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
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
            filteredTracks = listOf(sampleTrack),
            onAddTracksClick = {},
            onTrackClick = {},
            onTrackLongClick = {},
            onSwipeAction = {},
            onShufflePlay = {}
        )
    }
}

// --- UI State ---
data class PlaylistDetailUiState(
    val playlist: PlaylistEntity? = null,
    val tracks: List<Track> = emptyList(),
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val selectedTracks: Set<Track> = emptySet()
)
