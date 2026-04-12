package com.nkds.hosikoouma.jasmine.ui.screens

import android.app.Activity
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.data.ShareHelper
import com.nkds.hosikoouma.jasmine.ui.components.AddToPlaylistDialog
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.JasmineProgressBar
import com.nkds.hosikoouma.jasmine.ui.components.PlayerBackground
import com.nkds.hosikoouma.jasmine.ui.components.TrackInfoBottomSheet
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
    
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showTrackInfo by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

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
                    modifier = Modifier.size(72.dp).graphicsLayer { scaleX = playPauseScale; scaleY = scaleX },
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
                                onClick = { /* TODO */ }
                            )
                        }
                        item {
                            ActionCard(
                                icon = Icons.Rounded.GraphicEq,
                                label = "Pitch",
                                onClick = { /* TODO */ }
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
                        onClick = {
                            showDeleteDialog = false
                            currentTrack?.let { track ->
                                viewModel.prepareForDeletion(listOf(track))
                                trackViewModel.deleteTracks(listOf(track))
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
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
fun ActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
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
