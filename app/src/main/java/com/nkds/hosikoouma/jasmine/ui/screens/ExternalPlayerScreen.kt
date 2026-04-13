package com.nkds.hosikoouma.jasmine.ui.screens

import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.nkds.hosikoouma.jasmine.core.models.ProgressBarStyle
import com.nkds.hosikoouma.jasmine.core.utils.FormatUtils
import com.nkds.hosikoouma.jasmine.core.utils.VibrationUtils
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.JasmineProgressBar
import com.nkds.hosikoouma.jasmine.ui.components.PlayerBackground
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- UI State ---
data class ExternalPlayerUiState(
    val title: String = "",
    val artist: String = "",
    val artwork: ByteArray? = null,
    val isPlaying: Boolean = false,
    val progress: Long = 0,
    val duration: Long = 0,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1f,
    val playbackPitch: Float = 1f,
    val progressStyle: ProgressBarStyle = ProgressBarStyle.STANDARD
)

// --- Stateful Screen ---
@Composable
fun ExternalPlayerScreen(
    player: Player,
    title: String,
    artist: String,
    artwork: ByteArray?,
    onClose: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var progress by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }
    var speed by remember { mutableFloatStateOf(player.playbackParameters.speed) }
    var pitch by remember { mutableFloatStateOf(player.playbackParameters.pitch) }

    val settingsViewModel: SettingsViewModel = viewModel()
    val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) duration = player.duration }
            override fun onRepeatModeChanged(mode: Int) { repeatMode = mode }
            override fun onPlaybackParametersChanged(params: PlaybackParameters) {
                speed = params.speed
                pitch = params.pitch
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            progress = player.currentPosition
            delay(1000)
        }
    }

    val uiState = ExternalPlayerUiState(
        title = title, artist = artist, artwork = artwork,
        isPlaying = isPlaying, progress = progress, duration = duration,
        repeatMode = repeatMode, playbackSpeed = speed, playbackPitch = pitch,
        progressStyle = settings.progressBarStyle
    )

    ExternalPlayerContent(
        uiState = uiState,
        onClose = onClose,
        onTogglePlayPause = { if (player.isPlaying) player.pause() else player.play() },
        onSeek = { player.seekTo(it) },
        onToggleRepeat = { player.repeatMode = if (player.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF },
        onSetSpeed = { player.playbackParameters = PlaybackParameters(it, player.playbackParameters.pitch) },
        onSetPitch = { player.playbackParameters = PlaybackParameters(player.playbackParameters.speed, it) }
    )
}

// --- Stateless Content ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalPlayerContent(
    uiState: ExternalPlayerUiState,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleRepeat: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetPitch: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPitchSheet by remember { mutableStateOf(false) }

    // Animations
    val animatedOffset = remember { Animatable(1000f) }
    LaunchedEffect(Unit) { animatedOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }

    var isArtMinimized by remember { mutableStateOf(!uiState.isPlaying) }
    LaunchedEffect(uiState.isPlaying) { isArtMinimized = !uiState.isPlaying }
    val artScale by animateFloatAsState(targetValue = if (isArtMinimized) 0.8f else 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))

    BackHandler { onClose() }

    Box(
        modifier = Modifier.fillMaxSize().graphicsLayer { translationY = animatedOffset.value.coerceAtLeast(0f) }
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (animatedOffset.value > 300) scope.launch { animatedOffset.animateTo(2500f, tween(200)); onClose() }
                        else scope.launch { animatedOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                    },
                    onVerticalDrag = { change, dragAmount -> change.consume(); scope.launch { animatedOffset.snapTo(animatedOffset.value + dragAmount) } }
                )
            }
    ) {
        PlayerBackground(albumArtUri = null)

        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Spacer(modifier = Modifier.weight(0.2f))

            ExternalArtworkBox(uiState.artwork, artScale)

            Spacer(modifier = Modifier.weight(0.3f))

            ExternalInfoSection(uiState.title, uiState.artist)

            Spacer(modifier = Modifier.height(16.dp))

            ExternalProgressSection(uiState, onSeek)

            Spacer(modifier = Modifier.height(8.dp))

            ExternalControlsSection(
                uiState = uiState,
                onToggleRepeat = onToggleRepeat,
                onShowSpeed = { showSpeedSheet = true },
                onShowPitch = { showPitchSheet = true },
                onTogglePlayPause = onTogglePlayPause,
                onClose = onClose
            )

            Spacer(modifier = Modifier.weight(0.5f))
        }

        if (showSpeedSheet) {
            ExternalParameterSheet(title = "Playback Speed", value = uiState.playbackSpeed, valueRange = 0.25f..2.0f, steps = 6, icon = Icons.Rounded.Speed, onValueChange = onSetSpeed, onDismiss = { showSpeedSheet = false }, valueFormatter = { "%.2fx".format(it) })
        }
        if (showPitchSheet) {
            ExternalParameterSheet(title = "Playback Pitch", value = uiState.playbackPitch, valueRange = 0.5f..2.0f, steps = 5, icon = Icons.Rounded.GraphicEq, onValueChange = onSetPitch, onDismiss = { showPitchSheet = false }, valueFormatter = { "%.2f".format(it) })
        }
    }
}

// --- Internal Components ---

@Composable
private fun ExternalArtworkBox(artwork: ByteArray?, scale: Float) {
    Box(modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(1f), contentAlignment = Alignment.Center) {
        AlbumArt(
            albumArtUri = artwork,
            modifier = Modifier.fillMaxWidth(scale / 0.9f).aspectRatio(1f),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun ExternalInfoSection(title: String, artist: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.basicMarquee())
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.basicMarquee())
    }
}

@Composable
private fun ExternalProgressSection(uiState: ExternalPlayerUiState, onSeek: (Long) -> Unit) {
    var sliderValue by remember { mutableFloatStateOf(uiState.progress.toFloat()) }
    LaunchedEffect(uiState.progress) { sliderValue = uiState.progress.toFloat() }

    Column {
        if (uiState.progressStyle == ProgressBarStyle.STANDARD) {
            Slider(
                value = sliderValue, onValueChange = { sliderValue = it },
                onValueChangeFinished = { onSeek(sliderValue.toLong()) },
                valueRange = 0f..uiState.duration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )
        } else {
            JasmineProgressBar(value = sliderValue, onValueChange = { sliderValue = it }, onValueChangeFinished = { onSeek(sliderValue.toLong()) }, valueRange = 0f..uiState.duration.toFloat().coerceAtLeast(1f), style = uiState.progressStyle, isPlaying = uiState.isPlaying)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(FormatUtils.formatTime(sliderValue.toLong()), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(FormatUtils.formatTime(uiState.duration), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ExternalControlsSection(
    uiState: ExternalPlayerUiState,
    onToggleRepeat: () -> Unit,
    onShowSpeed: () -> Unit,
    onShowPitch: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onClose: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleRepeat) {
            Icon(if (uiState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, null, tint = if (uiState.repeatMode == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onShowSpeed) {
            Icon(Icons.Rounded.Speed, null, tint = if (uiState.playbackSpeed != 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
        Surface(
            onClick = { VibrationUtils.performLongPressHaptic(haptic); onTogglePlayPause() },
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(if (uiState.isPlaying) 50 else 25),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
            }
        }
        IconButton(onClick = onShowPitch) {
            Icon(Icons.Rounded.GraphicEq, null, tint = if (uiState.playbackPitch != 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, null, tint = if (uiState.playbackPitch != 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExternalParameterSheet(
    title: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, steps: Int,
    icon: ImageVector, onValueChange: (Float) -> Unit, onDismiss: () -> Unit, valueFormatter: (Float) -> String
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp)); Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp)); Text(text = valueFormatter(value), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(32.dp))
            Slider(value = value, onValueChange = { if (it != value) { VibrationUtils.tickVibrate(vibrator); onValueChange(it) } }, valueRange = valueRange, steps = steps)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { VibrationUtils.tickVibrate(vibrator); onValueChange(1.0f) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) { Text("Reset to Default", fontWeight = FontWeight.Bold) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExternalPlayerPreview() {
    JasmineTheme {
        ExternalPlayerContent(
            uiState = ExternalPlayerUiState(title = "External Audio", artist = "Downloaded File", isPlaying = true, progress = 5000, duration = 180000),
            onClose = {}, onTogglePlayPause = {}, onSeek = {}, onToggleRepeat = {}, onSetSpeed = {}, onSetPitch = {}
        )
    }
}
