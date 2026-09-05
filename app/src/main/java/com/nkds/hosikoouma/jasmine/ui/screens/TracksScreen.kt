package com.nkds.hosikoouma.jasmine.ui.screens

import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkds.hosikoouma.jasmine.R
import com.nkds.hosikoouma.jasmine.core.utils.VibrationUtils
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.ExpressiveRefreshIndicator
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.ui.components.simpleVerticalScrollbar
import com.nkds.hosikoouma.jasmine.ui.components.vibrateClick
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

// --- UI State ---
data class TracksUiState(
    val tracks: List<Track> = emptyList(),
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoaded: Boolean = false,
    val searchQuery: String = "",
    val isFavoritesMode: Boolean = false,
    val selectedTracks: Set<Track> = emptySet()
)

// --- Stateful Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksScreen(
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    selectedTracks: Set<Track>,
    onToggleTrackSelection: (Track) -> Unit
) {
    val isFavoritesMode by trackViewModel.isFavoritesMode.collectAsStateWithLifecycle()
    
    val tracks by if (isFavoritesMode) {
        trackViewModel.favoriteTracks.collectAsStateWithLifecycle()
    } else {
        trackViewModel.filteredTracks.collectAsStateWithLifecycle()
    }
    
    val currentTrack by playerViewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val isRefreshing by trackViewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoaded by trackViewModel.isLoaded.collectAsStateWithLifecycle()
    val searchQuery by trackViewModel.searchQuery.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        playerViewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val uiState = TracksUiState(
        tracks = tracks,
        currentTrack = currentTrack,
        isPlaying = isPlaying,
        isRefreshing = isRefreshing,
        isLoaded = isLoaded,
        searchQuery = searchQuery,
        isFavoritesMode = isFavoritesMode,
        selectedTracks = selectedTracks
    )

    TracksContent(
        uiState = uiState,
        onRefresh = trackViewModel::loadTracks,
        onToggleFavoritesMode = { trackViewModel.setFavoritesMode(it) },
        onTrackClick = { index ->
            if (selectedTracks.isNotEmpty()) {
                onToggleTrackSelection(uiState.tracks[index])
            } else {
                playerViewModel.playTracks(uiState.tracks, index)
                onNavigateToPlayer()
            }
        },
        onTrackLongClick = onToggleTrackSelection,
        onSwipeAction = { track ->
            if (track.isManual) {
                playerViewModel.removeFromQueue(track)
            } else {
                playerViewModel.addToQueue(track, showToast = true)
            }
        },
        onShufflePlay = {
            if (uiState.tracks.isNotEmpty()) {
                playerViewModel.shuffleAndPlay(uiState.tracks)
                onNavigateToPlayer()
            }
        }
    )
}

// --- Stateless Content ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksContent(
    uiState: TracksUiState,
    onRefresh: () -> Unit,
    onToggleFavoritesMode: (Boolean) -> Unit,
    onTrackClick: (Int) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onSwipeAction: (Track) -> Unit,
    onShufflePlay: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(uiState.isFavoritesMode) { listState.scrollToItem(0) }
    LaunchedEffect(uiState.searchQuery) { if (uiState.searchQuery.isNotEmpty()) listState.scrollToItem(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                isRefreshing = uiState.isRefreshing,
                state = pullToRefreshState,
                onRefresh = onRefresh
            )
    ) {
        AnimatedContent(
            targetState = Triple(uiState.isLoaded, uiState.tracks.isEmpty(), uiState.isFavoritesMode),
            transitionSpec = {
                (fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) + 
                 scaleIn(initialScale = 0.95f, animationSpec = tween(400, easing = LinearOutSlowInEasing)))
                    .togetherWith(fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)))
            },
            label = "tracksContentTransition"
        ) { (isLoaded, isEmpty, isFav) ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (!isLoaded && isEmpty) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (isEmpty) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (uiState.searchQuery.isEmpty()) {
                            if (isFav) stringResource(R.string.no_favorites) else stringResource(R.string.no_tracks)
                        } else stringResource(R.string.nothing_found))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .simpleVerticalScrollbar(listState),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 70.dp, bottom = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.tracks,
                            key = { _, track -> track.id },
                            contentType = { _, _ -> "track" }
                        ) { index, track ->
                            val isSelected = uiState.selectedTracks.contains(track)
                            SwipeableTrackCard(
                                track = track,
                                isCurrent = uiState.currentTrack?.id == track.id,
                                isPlaying = uiState.isPlaying,
                                isSelected = isSelected,
                                isManualMarkingEnabled = true,
                                enabled = uiState.selectedTracks.isEmpty(),
                                onSwipeAction = { onSwipeAction(track) },
                                onClick = {
                                    if (uiState.selectedTracks.isNotEmpty()) {
                                        VibrationUtils.selectionVibrate(vibrator)
                                    }
                                    onTrackClick(index)
                                },
                                onLongClick = {
                                    VibrationUtils.selectionVibrate(vibrator)
                                    onTrackLongClick(track)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Кастомный индикатор
        ExpressiveRefreshIndicator(
            state = pullToRefreshState,
            isRefreshing = uiState.isRefreshing,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (uiState.selectedTracks.isEmpty()) {
            ShuffleButton(
                modifier = Modifier.padding(top = 16.dp, start = 16.dp).align(Alignment.TopStart),
                onShuffle = {
                    vibrateClick(context)
                    onShufflePlay()
                }
            )

            ModeSelector(
                modifier = Modifier.padding(top = 16.dp, end = 16.dp).align(Alignment.TopEnd),
                isFavoritesMode = uiState.isFavoritesMode,
                onModeChange = {
                    vibrateClick(context)
                    onToggleFavoritesMode(it)
                }
            )
        }
    }
}

// --- Internal Components ---

@Composable
fun ShuffleButton(modifier: Modifier, onShuffle: () -> Unit) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        tonalElevation = 8.dp,
        shadowElevation = 6.dp
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .padding(4.dp).height(40.dp).width(80.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onShuffle),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Shuffle, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ModeSelector(modifier: Modifier, isFavoritesMode: Boolean, onModeChange: (Boolean) -> Unit) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        tonalElevation = 8.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeToggleButton(
                selected = !isFavoritesMode,
                icon = Icons.Rounded.MusicNote,
                onClick = { onModeChange(false) }
            )
            ModeToggleButton(
                selected = isFavoritesMode,
                icon = Icons.Rounded.Favorite,
                onClick = { onModeChange(true) }
            )
        }
    }
}

@Composable
fun ModeToggleButton(
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "bg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "icon"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun TracksPreview() {
    JasmineTheme {
        TracksContent(
            uiState = TracksUiState(
                tracks = listOf(
                    Track(1, "Song 1", "Artist 1", "Album 1", 200000, android.net.Uri.EMPTY, null),
                    Track(2, "Song 2", "Artist 2", "Album 2", 240000, android.net.Uri.EMPTY, null)
                ),
                isLoaded = true
            ),
            onRefresh = {},
            onToggleFavoritesMode = {},
            onTrackClick = {},
            onTrackLongClick = {},
            onSwipeAction = {},
            onShufflePlay = {}
        )
    }
}
