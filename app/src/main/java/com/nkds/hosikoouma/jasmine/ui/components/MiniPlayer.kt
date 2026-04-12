package com.nkds.hosikoouma.jasmine.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch

@Composable
fun MiniPlayer(
    viewModel: PlayerViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val currentStation by viewModel.currentRadioStation.collectAsStateWithLifecycle()
    val isRadioMode by viewModel.isRadioMode.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    
    val radioTrackTitle by viewModel.radioTrackTitle.collectAsStateWithLifecycle()
    val radioTrackArtist by viewModel.radioTrackArtist.collectAsStateWithLifecycle()

    if (currentTrack == null && currentStation == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .height(64.dp)
            .fillMaxWidth()
            .draggable(
                enabled = !isRadioMode,
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    scope.launch { offsetX.snapTo(offsetX.value + delta) }
                },
                onDragStopped = {
                    if (offsetX.value > 160) {
                        viewModel.skipToNext()
                        vibrateClick(context)
                    } else if (offsetX.value < -160) {
                        viewModel.skipToPrevious()
                        vibrateClick(context)
                    }
                    scope.launch {
                        offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                    }
                }
            )
            .graphicsLayer {
                translationX = offsetX.value
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .bouncingClickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isRadioMode) {
                MiniPlayerProgress(viewModel)
            } else if (isPlaying) {
                RadioPulseBackground()
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRadioMode) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Radio,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    AlbumArt(
                        albumArtUri = currentTrack?.albumArtUri,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isRadioMode) {
                            radioTrackTitle ?: currentStation?.name ?: ""
                        } else {
                            currentTrack?.title ?: ""
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isRadioMode) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "LIVE",
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isRadioMode) {
                                radioTrackArtist ?: "Radio Stream"
                            } else {
                                currentTrack?.artist ?: ""
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = { 
                        vibrateClick(context)
                        viewModel.togglePlayPause() 
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerProgress(viewModel: PlayerViewModel) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    
    val progressFactor = remember(progress, duration) {
        if (duration > 0) progress.toFloat() / duration.toFloat() else 0f
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(progressFactor)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    )
}

@Composable
private fun RadioPulseBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "radioPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radioPulse"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha))
    )
}
