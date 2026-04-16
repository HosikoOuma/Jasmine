package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkds.hosikoouma.jasmine.core.models.ProgressBarStyle
import com.nkds.hosikoouma.jasmine.core.utils.VibrationUtils
import com.nkds.hosikoouma.jasmine.data.RadioStation
import com.nkds.hosikoouma.jasmine.ui.components.JasmineProgressBar
import com.nkds.hosikoouma.jasmine.ui.components.PlayerBackground
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

// --- UI State ---
data class RadioPlayerUiState(
    val stationName: String = "",
    val trackTitle: String? = null,
    val trackArtist: String? = null,
    val isPlaying: Boolean = false,
    val progressStyle: ProgressBarStyle = ProgressBarStyle.STANDARD,
    val systemVolume: Float = 0f
)

// --- Stateful Screen ---
@Composable
fun RadioPlayerScreen(
    station: RadioStation,
    playerViewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val radioTrackTitle by playerViewModel.radioTrackTitle.collectAsStateWithLifecycle()
    val radioTrackArtist by playerViewModel.radioTrackArtist.collectAsStateWithLifecycle()
    
    val settingsViewModel: SettingsViewModel = viewModel()
    val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()

    val systemVolume by playerViewModel.systemVolume.collectAsStateWithLifecycle()

    val uiState = RadioPlayerUiState(
        stationName = station.name,
        trackTitle = radioTrackTitle,
        trackArtist = radioTrackArtist,
        isPlaying = isPlaying,
        progressStyle = settings.progressBarStyle,
        systemVolume = systemVolume
    )

    RadioPlayerContent(
        uiState = uiState,
        onClose = onClose,
        onTogglePlayPause = playerViewModel::togglePlayPause,
        onSkipNext = playerViewModel::skipToNext,
        onSkipPrevious = playerViewModel::skipToPrevious,
        onSetSystemVolume = playerViewModel::setSystemVolume
    )
}

// --- Stateless Content ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioPlayerContent(
    uiState: RadioPlayerUiState,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSetSystemVolume: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Back Gesture states
    var playerBackProgress by remember { mutableFloatStateOf(0f) }
    var isBackingPlayer by remember { mutableStateOf(false) }

    PredictiveBackHandler(enabled = true) { progressFlow ->
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

    var isArtMinimized by remember { mutableStateOf(!uiState.isPlaying) }
    LaunchedEffect(uiState.isPlaying) { if (uiState.isPlaying) isArtMinimized = false }

    val artScale by animateFloatAsState(
        targetValue = if (isArtMinimized) 0.8f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "artScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = if (isBackingPlayer) playerBackProgress * size.height else animatedOffset.value.coerceAtLeast(0f)
            }
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (animatedOffset.value > 300) {
                            scope.launch { animatedOffset.animateTo(2500f, tween(200)); onClose() }
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
        PlayerBackground(albumArtUri = null)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var localVolume by remember { mutableFloatStateOf(uiState.systemVolume) }
            LaunchedEffect(uiState.systemVolume) { localVolume = uiState.systemVolume }
            RadioLiveIndicator()

            Spacer(modifier = Modifier.weight(0.2f))

            RadioArtwork(artScale)

            Spacer(modifier = Modifier.weight(0.3f))

            RadioInfoSection(
                title = uiState.trackTitle ?: uiState.stationName,
                artist = uiState.trackArtist ?: "Radio Stream"
            )

            Spacer(modifier = Modifier.height(16.dp))

            RadioProgressSection(uiState.progressStyle, uiState.isPlaying)

            Spacer(modifier = Modifier.height(8.dp))

            RadioControlsSection(
                isPlaying = uiState.isPlaying,
                onTogglePlayPause = {
                    onTogglePlayPause()
                    if (uiState.isPlaying) isArtMinimized = true
                },
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeDown, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = localVolume,
                    onValueChange = {
                        localVolume = it
                        onSetSystemVolume(it)
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

// --- Internal Components ---

@Composable
private fun RadioLiveIndicator() {
    Box(modifier = Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
            shape = CircleShape,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Radio, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RadioArtwork(artScale: Float) {
    Box(modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(1f), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(artScale / 0.9f).aspectRatio(1f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Radio, null, modifier = Modifier.size(120.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun RadioInfoSection(title: String, artist: String) {
    Column(modifier = Modifier.fillMaxWidth().height(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.basicMarquee())
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.basicMarquee())
    }
}

@Composable
private fun RadioProgressSection(progressStyle: ProgressBarStyle, isPlaying: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().height(84.dp)) {
        if (progressStyle == ProgressBarStyle.STANDARD) {
            Slider(
                value = 1f, onValueChange = {}, valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
        } else {
            JasmineProgressBar(value = 1f, onValueChange = {}, onValueChangeFinished = {}, valueRange = 0f..1f, style = progressStyle, isPlaying = isPlaying)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("00:00", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text("∞", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
        }
    }
}

@Composable
private fun RadioControlsSection(
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        RadioAnimatedControlIcon(Icons.Rounded.SkipPrevious, 44.dp) {
            VibrationUtils.performLongPressHaptic(haptic)
            onSkipPrevious()
        }
        RadioAnimatedControlIcon(Icons.Rounded.SkipNext, 44.dp) {
            VibrationUtils.performLongPressHaptic(haptic)
            onSkipNext()
        }

        val playPauseInteractionSource = remember { MutableInteractionSource() }
        val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
        val playPauseScale by animateFloatAsState(targetValue = if (isPlayPausePressed) 0.9f else 1f, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "playPauseScale")
        val cornerPercent by animateIntAsState(targetValue = if (isPlaying) 50 else 25, animationSpec = tween(500, easing = LinearOutSlowInEasing), label = "cornerAnimation")

        Surface(
            onClick = { VibrationUtils.performLongPressHaptic(haptic); onTogglePlayPause() },
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
fun RadioAnimatedControlIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: androidx.compose.ui.unit.Dp = 28.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "iconScale")

    IconButton(onClick = onClick, interactionSource = interactionSource, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(size))
    }
}

@Preview(showBackground = true)
@Composable
fun RadioPlayerPreview() {
    JasmineTheme {
        RadioPlayerContent(
            uiState = RadioPlayerUiState(
                stationName = "Jasmine Rocks",
                trackTitle = "Heavy Metal track",
                trackArtist = "Unknown Artist",
                isPlaying = true
            ),
            onClose = {},
            onTogglePlayPause = {},
            onSkipNext = {},
            onSkipPrevious = {},
            onSetSystemVolume = {}
        )
    }
}
