package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.JasmineProgressBar
import com.nkds.hosikoouma.jasmine.ui.components.PlayerBackground
import com.nkds.hosikoouma.jasmine.ui.components.TrackInfoBottomSheet
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.ProgressBarStyle
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val shuffleEnabled by viewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isFavorite by viewModel.isCurrentFavorite.collectAsState()
    val systemVolume by viewModel.systemVolume.collectAsState()
    
    val settingsViewModel: SettingsViewModel = viewModel()
    val progressStyleStr by settingsViewModel.progressBarStyle.collectAsState()
    val progressStyle = try {
        ProgressBarStyle.valueOf(progressStyleStr)
    } catch (e: Exception) {
        ProgressBarStyle.STANDARD
    }
    
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showTrackInfo by remember { mutableStateOf(false) }

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

    var queueBackProgress by remember { mutableFloatStateOf(0f) }
    var isBackingQueue by remember { mutableStateOf(false) }

    var lyricsBackProgress by remember { mutableFloatStateOf(0f) }
    var isBackingLyrics by remember { mutableStateOf(false) }

    PredictiveBackHandler(enabled = showQueue) { progressFlow ->
        try {
            progressFlow.collect { }
            showQueue = false
        } catch (e: Exception) { }
    }

    PredictiveBackHandler(enabled = showLyrics) { progressFlow ->
        try {
            progressFlow.collect { }
            showLyrics = false
        } catch (e: Exception) { }
    }

    PredictiveBackHandler(enabled = !showQueue && !showLyrics && !showMoreActions && !showTrackInfo) { progressFlow ->
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

    var sliderValue by remember { mutableFloatStateOf(0f) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(progress) {
        val now = System.currentTimeMillis()
        if (now - lastSeekTime > 1000L) {
            sliderValue = progress.toFloat()
        }
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
                        if (!showQueue && !showLyrics && !showMoreActions && !showTrackInfo) {
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

            Column(modifier = Modifier.fillMaxWidth().height(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(currentTrack?.title ?: "Unknown Title", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.basicMarquee())
                Spacer(modifier = Modifier.height(4.dp))
                Text(currentTrack?.artist ?: "Unknown Artist", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.basicMarquee())
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth().height(84.dp)) {
                if (progressStyle == ProgressBarStyle.STANDARD) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { 
                            sliderValue = it
                            lastSeekTime = System.currentTimeMillis()
                        },
                        onValueChangeFinished = {
                            viewModel.seekTo(sliderValue.toLong())
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
                            viewModel.seekTo(sliderValue.toLong())
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                AnimatedControlIcon(icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, tint = if (repeatMode == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.toggleRepeatMode() })
                AnimatedControlIcon(icon = Icons.Rounded.Shuffle, tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.toggleShuffle() } )
                AnimatedControlIcon(icon = Icons.Rounded.SkipPrevious, size = 44.dp, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.skipToPrevious() })
                AnimatedControlIcon(icon = Icons.Rounded.SkipNext, size = 44.dp, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.skipToNext() })

                val playPauseInteractionSource = remember { MutableInteractionSource() }
                val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
                val playPauseScale by animateFloatAsState(targetValue = if (isPlayPausePressed) 0.9f else 1f, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "playPauseScale")
                val cornerPercent by animateIntAsState(targetValue = if (isPlaying) 50 else 25, animationSpec = tween(500, easing = LinearOutSlowInEasing), label = "cornerAnimation")

                Surface(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val willPause = isPlaying
                        viewModel.togglePlayPause()
                        isAlbumArtMinimized = willPause
                    },
                    interactionSource = playPauseInteractionSource,
                    modifier = Modifier.size(72.dp).graphicsLayer { scaleX = playPauseScale; scaleY = playPauseScale },
                    shape = RoundedCornerShape(cornerPercent),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), 
                horizontalArrangement = Arrangement.SpaceAround, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedControlIcon(Icons.AutoMirrored.Rounded.PlaylistPlay, size = 28.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showQueue = true })
                AnimatedControlIcon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, size = 26.dp, tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.toggleFavoriteCurrent() })
                AnimatedControlIcon(Icons.Rounded.Lyrics, size = 26.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showLyrics = true })
                AnimatedControlIcon(Icons.Rounded.MoreHoriz, size = 28.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showMoreActions = true })
            }
        }

        AnimatedVisibility(visible = showQueue, enter = slideInHorizontally(initialOffsetX = { -it }), exit = slideOutHorizontally(targetOffsetX = { -it })) {
            Box(modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (isBackingQueue) {
                        translationY = queueBackProgress * size.height
                    }
                }
                .pointerInput(Unit) { detectVerticalDragGestures { _, _ -> } }
            ) {
                QueueScreen(viewModel = viewModel, onClose = { showQueue = false })
            }
        }

        AnimatedVisibility(visible = showLyrics, enter = slideInHorizontally(initialOffsetX = { it }), exit = slideOutHorizontally(targetOffsetX = { it })) {
            Box(modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (isBackingLyrics) {
                        translationY = lyricsBackProgress * size.height
                    }
                }
                .pointerInput(Unit) { detectVerticalDragGestures { _, _ -> } }
            ) {
                LyricsScreen(viewModel = viewModel, onClose = { showLyrics = false })
            }
        }

        if (showMoreActions) {
            ModalBottomSheet(
                onDismissRequest = { showMoreActions = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = currentTrack?.title ?: "Track Actions",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    // Volume Control section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = systemVolume,
                            onValueChange = { viewModel.setSystemVolume(it) },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    ListItem(
                        headlineContent = { Text("Details") },
                        leadingContent = { Icon(Icons.Rounded.Info, null) },
                        modifier = Modifier.clickable { 
                            showMoreActions = false
                            showTrackInfo = true
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Add to Playlist") },
                        leadingContent = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                        modifier = Modifier.clickable { /* TODO */ }
                    )
                    ListItem(
                        headlineContent = { Text("View Artist") },
                        leadingContent = { Icon(Icons.Rounded.Person, null) },
                        modifier = Modifier.clickable { /* TODO */ }
                    )
                    ListItem(
                        headlineContent = { Text("Share Track") },
                        leadingContent = { Icon(Icons.Rounded.Share, null) },
                        modifier = Modifier.clickable { /* TODO */ }
                    )
                    ListItem(
                        headlineContent = { Text("Sleep Timer") },
                        leadingContent = { Icon(Icons.Rounded.Timer, null) },
                        modifier = Modifier.clickable { /* TODO */ }
                    )
                }
            }
        }

        if (showTrackInfo && currentTrack != null) {
            TrackInfoBottomSheet(
                track = currentTrack!!,
                onDismissRequest = { showTrackInfo = false }
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
