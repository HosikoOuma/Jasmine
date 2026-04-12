package com.nkds.hosikoouma.jasmine.ui.screens

import android.app.Activity
import android.os.Build
import android.os.VibrationEffect
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.data.ShareHelper
import com.nkds.hosikoouma.jasmine.ui.components.AddToPlaylistDialog
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.JasmineProgressBar
import com.nkds.hosikoouma.jasmine.ui.components.PlayerBackground
import com.nkds.hosikoouma.jasmine.ui.components.TrackInfoBottomSheet
import com.nkds.hosikoouma.jasmine.ui.components.bouncingClickable
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.ProgressBarStyle
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    trackViewModel: TrackViewModel,
    navController: NavController,
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
    
    val settingsViewModel: SettingsViewModel = viewModel()
    val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    
    val progressStyle = remember(settings.progressBarStyle) {
        try {
            ProgressBarStyle.valueOf(settings.progressBarStyle)
        } catch (e: Exception) {
            ProgressBarStyle.STANDARD
        }
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showTrackInfo by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPitchSheet by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            trackViewModel.loadTracks()
            Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        trackViewModel.pendingDeleteIntent.collect { intentSender ->
            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

    var isAlbumArtMinimized by remember { mutableStateOf(!isPlaying) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) isAlbumArtMinimized = false
    }

    val albumArtScale by animateFloatAsState(
        targetValue = if (isAlbumArtMinimized) 0.8f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "albumArtScale"
    )

    // Back Gesture states
    var playerBackProgress by remember { mutableFloatStateOf(0f) }
    var isBackingPlayer by remember { mutableStateOf(false) }

    PredictiveBackHandler(enabled = showQueue || showLyrics || showMoreActions || showTrackInfo || showSpeedSheet || showPitchSheet) { progressFlow ->
        try {
            progressFlow.collect { }
            showQueue = false
            showLyrics = false
            showMoreActions = false
            showTrackInfo = false
            showSpeedSheet = false
            showPitchSheet = false
        } catch (e: Exception) { }
    }

    PredictiveBackHandler(enabled = !showQueue && !showLyrics && !showMoreActions && !showTrackInfo && !showSpeedSheet && !showPitchSheet) { progressFlow ->
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
        animatedOffset.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        )
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
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (animatedOffset.value > 300) {
                            scope.launch {
                                animatedOffset.animateTo(2500f, tween(200))
                                onClose()
                            }
                        } else {
                            scope.launch { animatedOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (!showQueue && !showLyrics && !showMoreActions && !showTrackInfo && !showSpeedSheet && !showPitchSheet) {
                            change.consume()
                            scope.launch { animatedOffset.snapTo(animatedOffset.value + dragAmount) }
                        }
                    }
                )
            }
    ) {
        PlayerBackground(albumArtUri = currentTrack?.albumArtUri)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (currentTrack?.isManual == true) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), shape = CircleShape, modifier = Modifier.padding(top = 8.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("From Queue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                AlbumArt(
                    albumArtUri = currentTrack?.albumArtUri,
                    modifier = Modifier.fillMaxWidth(albumArtScale / 0.9f).aspectRatio(1f),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.3f))

            TrackInfoSection(title = currentTrack?.title, artist = currentTrack?.artist)

            Spacer(modifier = Modifier.height(16.dp))

            PlaybackProgressSection(
                progress = progress,
                duration = duration,
                progressStyle = progressStyle,
                isPlaying = isPlaying,
                onSeek = { viewModel.seekTo(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PlaybackControlsSection(
                isPlaying = isPlaying,
                repeatMode = repeatMode,
                shuffleEnabled = shuffleEnabled,
                onToggleRepeat = { viewModel.toggleRepeatMode() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onPrevious = { viewModel.skipToPrevious() },
                onNext = { viewModel.skipToNext() },
                onTogglePlayPause = {
                    val willPause = isPlaying
                    viewModel.togglePlayPause()
                    isAlbumArtMinimized = willPause
                }
            )

            Spacer(modifier = Modifier.weight(0.5f))

            BottomActionsSection(
                isFavorite = isFavorite,
                onShowQueue = { showQueue = true },
                onToggleFavorite = { viewModel.toggleFavoriteCurrent() },
                onShowLyrics = { showLyrics = true },
                onShowMore = { showMoreActions = true }
            )
        }

        // ... остальное (AnimatedVisibility, Dialogs) остается прежним, так как они не рекомпозируются часто
        AnimatedVisibility(visible = showQueue, enter = slideInHorizontally(initialOffsetX = { -it }), exit = slideOutHorizontally(targetOffsetX = { -it })) {
            QueueScreen(viewModel = viewModel, onClose = { showQueue = false })
        }

        AnimatedVisibility(visible = showLyrics, enter = slideInHorizontally(initialOffsetX = { it }), exit = slideOutHorizontally(targetOffsetX = { it })) {
            LyricsScreen(viewModel = viewModel, onClose = { showLyrics = false })
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
                                value = systemVolume,
                                onValueChange = { viewModel.setSystemVolume(it) },
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
                            ActionCard(
                                icon = Icons.Rounded.Speed,
                                label = "Speed",
                                onClick = {
                                    showMoreActions = false
                                    showSpeedSheet = true
                                }
                            )
                        }
                        item {
                            ActionCard(
                                icon = Icons.Rounded.GraphicEq,
                                label = "Pitch",
                                onClick = {
                                    showMoreActions = false
                                    showPitchSheet = true
                                }
                            )
                        }
                        item {
                            ActionCard(
                                icon = Icons.Rounded.Album,
                                label = "Album",
                                onClick = {
                                    currentTrack?.let { track ->
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
                                    currentTrack?.let { track ->
                                        val encoded = URLEncoder.encode(track.artist, StandardCharsets.UTF_8.toString())
                                        navController.navigate("artist_detail/$encoded")
                                        showMoreActions = false
                                        onClose()
                                    }
                                }
                            )
                        }
                        item {
                            ActionCard(
                                icon = Icons.Rounded.Queue,
                                label = "Add to Queue",
                                onClick = {
                                    currentTrack?.let { viewModel.addToQueue(it, showToast = true) }
                                    showMoreActions = false
                                }
                            )
                        }
                        item {
                            ActionCard(
                                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                label = "Playlist",
                                onClick = {
                                    showMoreActions = false
                                    showAddToPlaylistDialog = true
                                }
                            )
                        }
                        item {
                            ActionCard(
                                icon = Icons.Rounded.Info,
                                label = "Details",
                                onClick = {
                                    showMoreActions = false
                                    showTrackInfo = true
                                }
                            )
                        }
                        item {
                            ActionCard(
                                icon = Icons.Rounded.Share,
                                label = "Share",
                                onClick = {
                                    val track = currentTrack ?: return@ActionCard
                                    ShareHelper.shareTrack(context, track)
                                    showMoreActions = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncingClickable {
                                showMoreActions = false
                                showDeleteDialog = true
                            },
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Delete from Device", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showSpeedSheet) {
            val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
            ParameterAdjustmentSheet(
                title = "Playback Speed",
                value = speed,
                valueRange = 0.25f..2.0f,
                steps = 6,
                icon = Icons.Rounded.Speed,
                onValueChange = { viewModel.setPlaybackSpeed(it) },
                onReset = { viewModel.setPlaybackSpeed(1.0f) },
                onDismiss = { showSpeedSheet = false },
                valueFormatter = { "%.2fx".format(it) }
            )
        }

        if (showPitchSheet) {
            val pitch by viewModel.playbackPitch.collectAsStateWithLifecycle()
            ParameterAdjustmentSheet(
                title = "Playback Pitch",
                value = pitch,
                valueRange = 0.5f..2.0f,
                steps = 5,
                icon = Icons.Rounded.GraphicEq,
                onValueChange = { viewModel.setPlaybackPitch(it) },
                onReset = { viewModel.setPlaybackPitch(1.0f) },
                onDismiss = { showPitchSheet = false },
                valueFormatter = { "%.2f".format(it) }
            )
        }

        if (showAddToPlaylistDialog && currentTrack != null) {
            AddToPlaylistDialog(
                onDismissRequest = { showAddToPlaylistDialog = false },
                onPlaylistSelected = { playlistId ->
                    trackViewModel.addTrackToPlaylist(playlistId, currentTrack!!.id)
                    showAddToPlaylistDialog = false
                    Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
                },
                trackViewModel = trackViewModel
            )
        }

        if (showDeleteDialog && currentTrack != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Track") },
                text = { Text("Are you sure you want to delete \"${currentTrack?.title}\" from your device?") },
                confirmButton = {
                    TextButton(
                        modifier = Modifier.bouncingClickable {
                            showDeleteDialog = false
                            currentTrack?.let { track ->
                                viewModel.prepareForDeletion(listOf(track))
                                trackViewModel.deleteTracks(listOf(track))
                            }
                        },
                        onClick = { }
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            )
        }

        if (showTrackInfo && currentTrack != null) {
            TrackInfoBottomSheet(track = currentTrack!!, onDismissRequest = { showTrackInfo = false })
        }
    }
}

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
    progress: Long,
    duration: Long,
    progressStyle: ProgressBarStyle,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(progress) {
        val now = System.currentTimeMillis()
        if (now - lastSeekTime > 1000L) {
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
            Text(formatTime(sliderValue.toLong()), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(formatTime(duration), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
fun PlaybackControlsSection(
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePlayPause: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        AnimatedControlIcon(icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, tint = if (repeatMode == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleRepeat() })
        AnimatedControlIcon(icon = Icons.Rounded.Shuffle, tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleShuffle() } )
        AnimatedControlIcon(icon = Icons.Rounded.SkipPrevious, size = 44.dp, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onPrevious() })
        AnimatedControlIcon(icon = Icons.Rounded.SkipNext, size = 44.dp, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNext() })

        val playPauseInteractionSource = remember { MutableInteractionSource() }
        val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
        val playPauseScale by animateFloatAsState(targetValue = if (isPlayPausePressed) 0.9f else 1f, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "playPauseScale")
        val cornerPercent by animateIntAsState(targetValue = if (isPlaying) 50 else 25, animationSpec = tween(500, easing = LinearOutSlowInEasing), label = "cornerAnimation")

        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
        AnimatedControlIcon(Icons.AutoMirrored.Rounded.PlaylistPlay, size = 28.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onShowQueue() })
        AnimatedControlIcon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, size = 26.dp, tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleFavorite() })
        AnimatedControlIcon(Icons.Rounded.Lyrics, size = 26.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onShowLyrics() })
        AnimatedControlIcon(Icons.Rounded.MoreHoriz, size = 28.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onShowMore() })
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
                        tickVibrate(vibrator)
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
                                    tickVibrate(vibrator)
                                    onValueChange(preset)
                                }
                            },
                            label = { Text(valueFormatter(preset)) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.graphicsLayer {
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    tickVibrate(vibrator)
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

private fun tickVibrate(vibrator: Vibrator?) {
    if (vibrator == null) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(10, 100))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(10)
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
