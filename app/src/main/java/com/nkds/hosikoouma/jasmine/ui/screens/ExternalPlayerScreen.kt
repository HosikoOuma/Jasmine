package com.nkds.hosikoouma.jasmine.ui.screens

import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.JasmineProgressBar
import com.nkds.hosikoouma.jasmine.ui.components.PlayerBackground
import com.nkds.hosikoouma.jasmine.viewmodels.ProgressBarStyle
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.PlaybackParameters
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalPlayerScreen(
    player: Player,
    title: String,
    artist: String,
    artwork: ByteArray?, // Изменено на ByteArray?
    onClose: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var progress by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }
    var speed by remember { mutableFloatStateOf(player.playbackParameters.speed) }
    var pitch by remember { mutableFloatStateOf(player.playbackParameters.pitch) }

    val settingsViewModel: SettingsViewModel = viewModel()
    val progressStyleStr by settingsViewModel.progressBarStyle.collectAsState()
    val progressStyle = try { ProgressBarStyle.valueOf(progressStyleStr) } catch (e: Exception) { ProgressBarStyle.STANDARD }

    val scope = rememberCoroutineScope()

    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPitchSheet by remember { mutableStateOf(false) }

    // Анимация масштаба обложки
    var isAlbumArtMinimized by remember { mutableStateOf(!isPlaying) }
    LaunchedEffect(isPlaying) {
        isAlbumArtMinimized = !isPlaying
    }
    
    val albumArtScale by animateFloatAsState(
        targetValue = if (isAlbumArtMinimized) 0.8f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "albumArtScale"
    )

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) { 
                if (state == Player.STATE_READY) duration = player.duration 
            }
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

    val animatedOffset = remember { Animatable(1000f) }
    LaunchedEffect(Unit) {
        animatedOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
    }

    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .graphicsLayer { translationY = animatedOffset.value.coerceAtLeast(0f) }
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
                        change.consume()
                        scope.launch { animatedOffset.snapTo(animatedOffset.value + dragAmount) }
                    }
                )
            }
    ) {
        // Здесь мы передаем null в PlayerBackground, так как Uri больше нет.
        // Если вы захотите размытый фон из байтов, его нужно будет доработать в PlayerBackground.kt
        PlayerBackground(albumArtUri = null)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Spacer(modifier = Modifier.weight(0.2f))

            Box(
                modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                AlbumArt(
                    albumArtUri = artwork, // Теперь передаем байты
                    modifier = Modifier.fillMaxWidth(albumArtScale / 0.9f).aspectRatio(1f),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.3f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1, 
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = artist, 
                    style = MaterialTheme.typography.bodyLarge, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1, 
                    modifier = Modifier.basicMarquee()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                if (progressStyle == ProgressBarStyle.STANDARD) {
                    Slider(
                        value = progress.toFloat(),
                        onValueChange = { progress = it.toLong() },
                        onValueChangeFinished = { player.seekTo(progress) },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                } else {
                    JasmineProgressBar(
                        value = progress.toFloat(),
                        onValueChange = { progress = it.toLong() },
                        onValueChangeFinished = { player.seekTo(progress) },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        style = progressStyle,
                        isPlaying = isPlaying
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(progress), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(formatTime(duration), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { 
                    val newMode = if (repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    player.repeatMode = newMode
                }) {
                    Icon(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        null,
                        tint = if (repeatMode == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = { showSpeedSheet = true }) {
                    Icon(Icons.Rounded.Speed, null, tint = if (speed != 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }

                Surface(
                    onClick = { 
                        if (isPlaying) player.pause() else player.play()
                    },
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(if (isPlaying) 50 else 25),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 
                            null, 
                            tint = MaterialTheme.colorScheme.onPrimary, 
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(onClick = { showPitchSheet = true }) {
                    Icon(Icons.Rounded.GraphicEq, null, tint = if (pitch != 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))
        }

        if (showSpeedSheet) {
            ExternalParameterAdjustmentSheet(
                title = "Playback Speed",
                value = speed,
                valueRange = 0.25f..2.0f,
                steps = 6,
                icon = Icons.Rounded.Speed,
                onValueChange = { player.playbackParameters = PlaybackParameters(it, pitch) },
                onReset = { player.playbackParameters = PlaybackParameters(1.0f, pitch) },
                onDismiss = { showSpeedSheet = false },
                valueFormatter = { "%.2fx".format(it) }
            )
        }

        if (showPitchSheet) {
            ExternalParameterAdjustmentSheet(
                title = "Playback Pitch",
                value = pitch,
                valueRange = 0.5f..2.0f,
                steps = 5,
                icon = Icons.Rounded.GraphicEq,
                onValueChange = { player.playbackParameters = PlaybackParameters(speed, it) },
                onReset = { player.playbackParameters = PlaybackParameters(speed, 1.0f) },
                onDismiss = { showPitchSheet = false },
                valueFormatter = { "%.2f".format(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalParameterAdjustmentSheet(
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
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = valueFormatter(value), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(32.dp))
            Slider(
                value = value,
                onValueChange = { if (it != value) { externalTickVibrate(vibrator); onValueChange(it) } },
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                presets.forEach { preset ->
                    if (preset in valueRange) {
                        FilterChip(
                            selected = value == preset,
                            onClick = { if (value != preset) { externalTickVibrate(vibrator); onValueChange(preset) } },
                            label = { Text(valueFormatter(preset)) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { externalTickVibrate(vibrator); onReset() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) { Text("Reset to Default", fontWeight = FontWeight.Bold) }
        }
    }
}

private fun externalTickVibrate(vibrator: Vibrator?) {
    if (vibrator == null) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(10, 100))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(10)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
