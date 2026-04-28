package com.nkds.hosikoouma.jasmine.ui.screens

import android.app.Activity
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nkds.hosikoouma.jasmine.core.models.ProgressBarStyle
import com.nkds.hosikoouma.jasmine.core.utils.FormatUtils
import com.nkds.hosikoouma.jasmine.core.utils.VibrationUtils
import com.nkds.hosikoouma.jasmine.data.ShareHelper
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.*
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.absoluteValue

// --- UI State ---
data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val progress: Long = 0,
    val duration: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isFavorite: Boolean = false,
    val systemVolume: Float = 0f,
    val progressStyle: ProgressBarStyle = ProgressBarStyle.STANDARD,
    val playbackSpeed: Float = 1f,
    val playbackPitch: Float = 1f,
    val playlist: List<Track> = emptyList(),
    val sourceName: String? = null,
    val controlsOrder: List<String> = listOf("shuffle", "previous", "play_pause", "next", "repeat")
)

// --- Stateful Screen ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    trackViewModel: TrackViewModel,
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClose: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val shuffleEnabled by viewModel.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isCurrentFavorite.collectAsStateWithLifecycle()
    val systemVolume by viewModel.systemVolume.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val playbackPitch by viewModel.playbackPitch.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val currentSource by viewModel.currentSource.collectAsStateWithLifecycle()

    val settingsViewModel: SettingsViewModel = viewModel()
    val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()

    val uiState = PlayerUiState(
        currentTrack = currentTrack,
        isPlaying = isPlaying,
        progress = progress,
        duration = duration,
        shuffleEnabled = shuffleEnabled,
        repeatMode = repeatMode,
        isFavorite = isFavorite,
        systemVolume = systemVolume,
        progressStyle = settings.progressBarStyle,
        playbackSpeed = playbackSpeed,
        playbackPitch = playbackPitch,
        playlist = playlist,
        sourceName = currentSource,
        controlsOrder = settings.playerControlsOrder
    )

    PlayerContent(
        uiState = uiState,
        onClose = onClose,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onTogglePlayPause = viewModel::togglePlayPause,
        onSkipNext = viewModel::skipToNext,
        onSkipPrevious = viewModel::skipToPrevious,
        onSeek = viewModel::seekTo,
        onToggleShuffle = viewModel::toggleShuffle,
        onToggleRepeat = viewModel::toggleRepeatMode,
        onToggleFavorite = viewModel::toggleFavoriteCurrent,
        onSetSystemVolume = viewModel::setSystemVolume,
        onSetPlaybackSpeed = viewModel::setPlaybackSpeed,
        onSetPlaybackPitch = viewModel::setPlaybackPitch,
        onAddToQueue = { viewModel.addToQueue(it, showToast = true) },
        onPrepareForDeletion = viewModel::prepareForDeletion,
        onDeleteTracks = trackViewModel::deleteTracks,
        onLoadTracks = trackViewModel::loadTracks,
        onAddTrackToPlaylist = trackViewModel::addTrackToPlaylist,
        onSkipToItem = viewModel::skipToQueueItem,
        onSkipToMediaId = { /* no longer needed if skipToItem works */ },
        onShowToast = viewModel::showToast,
        navController = navController,
        queueScreen = { onCloseQueue -> QueueScreen(viewModel = viewModel, onClose = onCloseQueue) },
        lyricsScreen = { onCloseLyrics -> LyricsScreen(viewModel = viewModel, onClose = onCloseLyrics) },
        trackViewModel = trackViewModel
    )
}

// --- Stateless Content ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerContent(
    uiState: PlayerUiState,
    onClose: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetSystemVolume: (Float) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetPlaybackPitch: (Float) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onPrepareForDeletion: (List<Track>) -> Unit,
    onDeleteTracks: (List<Track>) -> Unit,
    onLoadTracks: () -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    onSkipToItem: (Int) -> Unit,
    onSkipToMediaId: (String) -> Unit = {},
    onShowToast: (Track?, ToastType, String?) -> Unit,
    navController: NavController = rememberNavController(),
    queueScreen: @Composable (onClose: () -> Unit) -> Unit = {},
    lyricsScreen: @Composable (onClose: () -> Unit) -> Unit = {},
    trackViewModel: TrackViewModel? = null 
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showTrackInfo by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showShareBottomSheet by remember { mutableStateOf(false) }

    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPitchSheet by remember { mutableStateOf(false) }

    // Блокируем взаимодействие, пока экран полностью не открылся или начал закрываться.
    val isInteractionEnabled = animatedVisibilityScope.transition.currentState == EnterExitState.Visible &&
            animatedVisibilityScope.transition.targetState == EnterExitState.Visible

    var isAlbumArtMinimized by remember { mutableStateOf(!uiState.isPlaying) }
    LaunchedEffect(uiState.isPlaying) {
        if (uiState.isPlaying) isAlbumArtMinimized = false
    }

    val albumArtScale by animateFloatAsState(
        targetValue = if (isAlbumArtMinimized) 0.8f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "albumArtScale"
    )

    // Pager State & Sync
    val currentIndex = remember(uiState.currentTrack, uiState.playlist) {
        uiState.playlist.indexOfFirst { it.uid == uiState.currentTrack?.uid }.coerceAtLeast(0)
    }
    
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { uiState.playlist.size.coerceAtLeast(1) }
    )

    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex) {
            pagerState.scrollToPage(currentIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != currentIndex && uiState.playlist.isNotEmpty()) {
            onSkipToItem(pagerState.currentPage)
        }
    }

    // Back Gesture states
    var playerBackProgress by remember { mutableFloatStateOf(0f) }
    var isBackingPlayer by remember { mutableStateOf(false) }

    PredictiveBackHandler(enabled = isInteractionEnabled && (showQueue || showLyrics || showMoreActions || showTrackInfo || showSpeedSheet || showPitchSheet || showShareBottomSheet)) { progressFlow ->
        try {
            progressFlow.collect { }
            showQueue = false
            showLyrics = false
            showMoreActions = false
            showTrackInfo = false
            showSpeedSheet = false
            showPitchSheet = false
            showShareBottomSheet = false
        } catch (e: Exception) { }
    }

    PredictiveBackHandler(enabled = isInteractionEnabled && !showQueue && !showLyrics && !showMoreActions && !showTrackInfo && !showSpeedSheet && !showPitchSheet && !showShareBottomSheet) { progressFlow ->
        try {
            isBackingPlayer = true
            progressFlow.collect { backEvent -> playerBackProgress = backEvent.progress }
            onClose()
        } catch (e: Exception) {
            isBackingPlayer = false
            playerBackProgress = 0f
        }
    }

    val animatedOffset = remember { Animatable(1000f) }
    LaunchedEffect(Unit) {
        animatedOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (!showQueue && !showLyrics && isBackingPlayer) {
                    translationY = playerBackProgress * size.height
                } else {
                    translationY = animatedOffset.value.coerceAtLeast(0f)
                }
            }
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) return@pointerInput
                
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (animatedOffset.value > 300) {
                            scope.launch {
                                animatedOffset.animateTo(2500f, tween(200))
                                onClose()
                            }
                        } else {
                            scope.launch { animatedOffset.snapTo(0f); animatedOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (!showQueue && !showLyrics && !showMoreActions && !showTrackInfo && !showSpeedSheet && !showPitchSheet && !showShareBottomSheet) {
                            change.consume()
                            scope.launch { animatedOffset.snapTo(animatedOffset.value + dragAmount) }
                        }
                    }
                )
            }
    ) {
        PlayerBackground(albumArtUri = uiState.currentTrack?.albumArtUri)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(72.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (uiState.currentTrack?.isManual == true) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            @Suppress("DEPRECATION")
                            Text("From Queue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (uiState.sourceName != null) {
                    Text(
                        text = uiState.sourceName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Album Art Section with Optimized Flat Pager Animation
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)), 
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1, 
                    pageSpacing = 24.dp,
                    key = { page -> "p_${uiState.playlist.getOrNull(page)?.uid ?: page}" }
                ) { page ->
                    val track = uiState.playlist.getOrNull(page) ?: uiState.currentTrack
                    val isCurrentPage = page == currentIndex

                    Box(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        with(sharedTransitionScope) {
                            AlbumArt(
                                albumArtUri = track?.albumArtUri,
                                updateTrigger = track?.dateModified ?: 0L,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (isCurrentPage) {
                                            Modifier.sharedElement(
                                                rememberSharedContentState(key = "album_art_${track?.id}"),
                                                animatedVisibilityScope = animatedVisibilityScope
                                            )
                                        } else Modifier
                                    )
                                    .graphicsLayer {
                                        scaleX = albumArtScale
                                        scaleY = albumArtScale
                                    },
                                shape = RoundedCornerShape(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            TrackInfoSection(title = uiState.currentTrack?.title, artist = uiState.currentTrack?.artist)

            Spacer(modifier = Modifier.height(16.dp))

            PlaybackProgressSection(
                currentTrack = uiState.currentTrack,
                progress = uiState.progress,
                duration = uiState.duration,
                progressStyle = uiState.progressStyle,
                isPlaying = uiState.isPlaying,
                onSeek = onSeek
            )

            Spacer(modifier = Modifier.height(8.dp))

            PlaybackControlsSection(
                isPlaying = uiState.isPlaying,
                repeatMode = uiState.repeatMode,
                shuffleEnabled = uiState.shuffleEnabled,
                controlsOrder = uiState.controlsOrder,
                onToggleRepeat = onToggleRepeat,
                onToggleShuffle = onToggleShuffle,
                onPrevious = onSkipPrevious,
                onNext = onSkipNext,
                onTogglePlayPause = {
                    onTogglePlayPause()
                    isAlbumArtMinimized = uiState.isPlaying
                }
            )

            Spacer(modifier = Modifier.weight(0.5f))

            BottomActionsSection(
                isFavorite = uiState.isFavorite,
                onShowQueue = { showQueue = true },
                onToggleFavorite = onToggleFavorite,
                onShowLyrics = { showLyrics = true },
                onShowMore = { showMoreActions = true }
            )
        }

        AnimatedVisibility(visible = showQueue, enter = slideInHorizontally(initialOffsetX = { -it }), exit = slideOutHorizontally(targetOffsetX = { -it })) {
            queueScreen { showQueue = false }
        }

        AnimatedVisibility(visible = showLyrics, enter = slideInHorizontally(initialOffsetX = { it }), exit = slideOutHorizontally(targetOffsetX = { it })) {
            lyricsScreen { showLyrics = false }
        }

        if (showMoreActions) {
            ModalBottomSheet(
                onDismissRequest = { showMoreActions = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    var localVolume by remember { mutableStateOf(uiState.systemVolume) }
                    LaunchedEffect(uiState.systemVolume) { localVolume = uiState.systemVolume }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeDown, null, modifier = Modifier.size(20.dp))
                            Slider(
                                value = localVolume,
                                onValueChange = { 
                                    localVolume = it
                                    onSetSystemVolume(it) 
                                },
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            )
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, modifier = Modifier.size(20.dp))
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        item {
                            ActionCard(icon = Icons.Rounded.Speed, label = "Speed", onClick = { showMoreActions = false; showSpeedSheet = true })
                        }
                        item {
                            ActionCard(icon = Icons.Rounded.GraphicEq, label = "Pitch", onClick = { showMoreActions = false; showPitchSheet = true })
                        }
                        item {
                            ActionCard(
                                icon = Icons.Rounded.Album,
                                label = "Album",
                                onClick = {
                                    uiState.currentTrack?.let { track ->
                                        val encoded = URLEncoder.encode(track.album, StandardCharsets.UTF_8.toString())
                                        navController.navigate("album_detail/$encoded")
                                        showMoreActions = false
                                        onClose()
                                    }
                                }
                            )
                        }
                        item {
                            ActionCard(
                                icon = Icons.Rounded.Person,
                                label = "Artist",
                                onClick = {
                                    uiState.currentTrack?.let { track ->
                                        val encoded = URLEncoder.encode(track.artist, StandardCharsets.UTF_8.toString())
                                        navController.navigate("artist_detail/$encoded")
                                        showMoreActions = false
                                        onClose()
                                    }
                                }
                            )
                        }
                        item {
                            ActionCard(icon = Icons.Rounded.Queue, label = "Add to Queue", onClick = { uiState.currentTrack?.let { onAddToQueue(it) }; showMoreActions = false })
                        }
                        item {
                            ActionCard(icon = Icons.AutoMirrored.Rounded.PlaylistAdd, label = "Playlist", onClick = { showMoreActions = false; showAddToPlaylistDialog = true })
                        }
                        item {
                            ActionCard(icon = Icons.Rounded.Info, label = "Details", onClick = { showMoreActions = false; showTrackInfo = true })
                        }
                        item {
                            ActionCard(
                                icon = Icons.Rounded.Share,
                                label = "Share",
                                onClick = {
                                    showMoreActions = false
                                    showShareBottomSheet = true
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        onClick = {
                            showMoreActions = false
                            showDeleteDialog = true
                        },
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            @Suppress("DEPRECATION")
                            Text("Delete from Device", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showSpeedSheet) {
            ParameterAdjustmentSheet(
                title = "Playback Speed",
                value = uiState.playbackSpeed,
                valueRange = 0.25f..2.0f,
                steps = 6,
                icon = Icons.Rounded.Speed,
                onValueChange = onSetPlaybackSpeed,
                onReset = { onSetPlaybackSpeed(1.0f) },
                onDismiss = { showSpeedSheet = false },
                valueFormatter = { "%.2fx".format(it) }
            )
        }

        if (showPitchSheet) {
            ParameterAdjustmentSheet(
                title = "Playback Pitch",
                value = uiState.playbackPitch,
                valueRange = 0.5f..2.0f,
                steps = 5,
                icon = Icons.Rounded.GraphicEq,
                onValueChange = onSetPlaybackPitch,
                onReset = { onSetPlaybackPitch(1.0f) },
                onDismiss = { showPitchSheet = false },
                valueFormatter = { "%.2f".format(it) }
            )
        }

        if (showAddToPlaylistDialog && uiState.currentTrack != null && trackViewModel != null) {
            AddToPlaylistDialog(
                onDismissRequest = { showAddToPlaylistDialog = false },
                onPlaylistSelected = { playlistId ->
                    onAddTrackToPlaylist(playlistId, uiState.currentTrack.id)
                    showAddToPlaylistDialog = false
                },
                trackViewModel = trackViewModel
            )
        }

        if (showDeleteDialog && uiState.currentTrack != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Track") },
                text = { Text("Are you sure you want to delete \"${uiState.currentTrack.title}\" from your device?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onPrepareForDeletion(listOf(uiState.currentTrack))
                            onDeleteTracks(listOf(uiState.currentTrack))
                        }
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            )
        }

        if (showTrackInfo && uiState.currentTrack != null) {
            TrackInfoBottomSheet(track = uiState.currentTrack, onDismissRequest = { showTrackInfo = false })
        }

        if (showShareBottomSheet && uiState.currentTrack != null) {
            TrackShareBottomSheet(
                track = uiState.currentTrack,
                onDismissRequest = { showShareBottomSheet = false }
            )
        }
    }
}

// --- Internal Components ---

@Composable
fun TrackInfoSection(title: String?, artist: String?) {
    Column(modifier = Modifier.fillMaxWidth().height(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title ?: "Unknown Title", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.basicMarquee())
        Spacer(modifier = Modifier.height(4.dp))
        Text(artist ?: "Unknown Artist", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.basicMarquee())
    }
}

@Composable
fun PlaybackProgressSection(
    currentTrack: Track?,
    progress: Long,
    duration: Long,
    progressStyle: ProgressBarStyle,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    var lastTrackId by remember { mutableLongStateOf(-1L) }

    LaunchedEffect(currentTrack?.id) {
        if (currentTrack?.id != lastTrackId) {
            sliderValue = 0f
            lastTrackId = currentTrack?.id ?: -1L
        }
    }

    LaunchedEffect(progress) {
        val now = System.currentTimeMillis()
        if (now - lastSeekTime > 1000L && currentTrack?.id == lastTrackId) {
            sliderValue = progress.toFloat()
        }
    }

    Column(modifier = Modifier.fillMaxWidth().height(84.dp)) {
        if (progressStyle == ProgressBarStyle.STANDARD) {
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    lastSeekTime = System.currentTimeMillis()
                },
                onValueChangeFinished = {
                    onSeek(sliderValue.toLong())
                    lastSeekTime = System.currentTimeMillis()
                },
                interactionSource = remember { MutableInteractionSource() },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
        } else {
            JasmineProgressBar(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    lastSeekTime = System.currentTimeMillis()
                },
                onValueChangeFinished = {
                    onSeek(sliderValue.toLong())
                    lastSeekTime = System.currentTimeMillis()
                },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                style = progressStyle,
                isPlaying = isPlaying
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(FormatUtils.formatTime(sliderValue.toLong()), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(FormatUtils.formatTime(duration), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
fun PlaybackControlsSection(
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    controlsOrder: List<String>,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePlayPause: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier.fillMaxWidth(), 
        horizontalArrangement = Arrangement.SpaceBetween, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        controlsOrder.forEach { key ->
            when (key) {
                "shuffle" -> AnimatedControlIcon(
                    icon = Icons.Rounded.Shuffle, 
                    tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, 
                    onClick = { VibrationUtils.performLongPressHaptic(haptic); onToggleShuffle() } 
                )
                "previous" -> AnimatedControlIcon(
                    icon = Icons.Rounded.SkipPrevious, 
                    size = 44.dp, 
                    onClick = { VibrationUtils.performLongPressHaptic(haptic); onPrevious() }
                )
                "play_pause" -> {
                    val playPauseInteractionSource = remember { MutableInteractionSource() }
                    val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
                    val playPauseScale by animateFloatAsState(targetValue = if (isPlayPausePressed) 0.7f else 1f, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "playPauseScale")
                    val cornerPercent by animateIntAsState(targetValue = if (isPlaying) 50 else 25, animationSpec = tween(500, easing = LinearOutSlowInEasing), label = "cornerAnimation")

                    Surface(
                        onClick = {
                            VibrationUtils.performLongPressHaptic(haptic)
                            onTogglePlayPause()
                        },
                        interactionSource = playPauseInteractionSource,
                        modifier = Modifier.size(72.dp).graphicsLayer { scaleX = playPauseScale; scaleY = scaleX },
                        shape = RoundedCornerShape(cornerPercent),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                        }
                    }
                }
                "next" -> AnimatedControlIcon(
                    icon = Icons.Rounded.SkipNext, 
                    size = 44.dp, 
                    onClick = { VibrationUtils.performLongPressHaptic(haptic); onNext() }
                )
                "repeat" -> AnimatedControlIcon(
                    icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, 
                    tint = if (repeatMode == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary, 
                    onClick = { VibrationUtils.performLongPressHaptic(haptic); onToggleRepeat() }
                )
            }
        }
    }
}

@Composable
fun BottomActionsSection(
    isFavorite: Boolean,
    onShowQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowLyrics: () -> Unit,
    onShowMore: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedControlIcon(Icons.AutoMirrored.Rounded.PlaylistPlay, size = 28.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = { VibrationUtils.performLongPressHaptic(haptic); onShowQueue() })
        AnimatedControlIcon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, size = 26.dp, tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { VibrationUtils.performLongPressHaptic(haptic); onToggleFavorite() })
        AnimatedControlIcon(Icons.Rounded.Lyrics, size = 26.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = { VibrationUtils.performLongPressHaptic(haptic); onShowLyrics() })
        AnimatedControlIcon(Icons.Rounded.MoreHoriz, size = 28.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = { VibrationUtils.performLongPressHaptic(haptic); onShowMore() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParameterAdjustmentSheet(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    icon: ImageVector,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    valueFormatter: (Float) -> String
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Slider(
                value = value,
                onValueChange = {
                    if (it != value) {
                        VibrationUtils.tickVibrate(vibrator)
                        onValueChange(it)
                    }
                },
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                presets.forEach { preset ->
                    if (preset in valueRange) {
                        FilterChip(
                            selected = value == preset,
                            onClick = {
                                if (value != preset) {
                                    VibrationUtils.tickVibrate(vibrator)
                                    onValueChange(preset)
                                }
                            },
                            label = { Text(valueFormatter(preset)) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    VibrationUtils.tickVibrate(vibrator)
                    onReset()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).bouncingClickable { onReset() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Reset to Default", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingClickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AnimatedControlIcon(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 28.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "iconScale")

    IconButton(onClick = onClick, interactionSource = interactionSource, modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true)
@Composable
fun PlayerPreview() {
    JasmineTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                PlayerContent(
                    uiState = PlayerUiState(
                        currentTrack = Track(
                            id = 1,
                            title = "Sample Song",
                            artist = "Sample Artist",
                            album = "Sample Album",
                            duration = 300000,
                            contentUri = android.net.Uri.EMPTY,
                            albumArtUri = null
                        ),
                        isPlaying = true,
                        progress = 120000,
                        duration = 300000,
                        isFavorite = true
                    ),
                    onClose = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility,
                    onTogglePlayPause = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeek = {},
                    onToggleShuffle = {},
                    onToggleRepeat = {},
                    onToggleFavorite = {},
                    onSetSystemVolume = {},
                    onSetPlaybackSpeed = {},
                    onSetPlaybackPitch = {},
                    onAddToQueue = {},
                    onPrepareForDeletion = {},
                    onDeleteTracks = {},
                    onLoadTracks = {},
                    onAddTrackToPlaylist = { _, _ -> },
                    onSkipToItem = {},
                    onShowToast = { _, _, _ -> }
                )
            }
        }
    }
}
