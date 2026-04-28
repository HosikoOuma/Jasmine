package com.nkds.hosikoouma.jasmine.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MiniPlayer(
    viewModel: PlayerViewModel,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val currentStation by viewModel.currentRadioStation.collectAsStateWithLifecycle()
    val isRadioMode by viewModel.isRadioMode.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    
    val radioTrackTitle by viewModel.radioTrackTitle.collectAsStateWithLifecycle()
    val radioTrackArtist by viewModel.radioTrackArtist.collectAsStateWithLifecycle()

    if (currentTrack == null && currentStation == null) return

    // Блокируем взаимодействие, если MiniPlayer находится в процессе анимации появления или исчезновения.
    // Это предотвращает конфликты SharedElement, если кликнуть слишком быстро.
    val isInteractionEnabled = animatedVisibilityScope.transition.currentState == EnterExitState.Visible &&
            animatedVisibilityScope.transition.targetState == EnterExitState.Visible

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    val maxDragUpPx = with(density) { 16.dp.toPx() }
    val maxDragDownPx = with(density) { 80.dp.toPx() }
    var hasVibratedOnLimit by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val dynamicCornerRadius by animateDpAsState(
        targetValue = if (offsetY.value < 0) {
            (16 + (abs(offsetY.value) / maxDragUpPx * 16)).dp
        } else 16.dp,
        label = "corners"
    )

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .height(64.dp)
            .fillMaxWidth()
            .pointerInput(isRadioMode, isInteractionEnabled) {
                if (!isInteractionEnabled) return@pointerInput
                
                var isVerticalDrag = false
                var isHorizontalDrag = false
                
                detectDragGestures(
                    onDragStart = {
                        isVerticalDrag = false
                        isHorizontalDrag = false
                        hasVibratedOnLimit = false
                    },
                    onDragEnd = {
                        scope.launch {
                            if (isVerticalDrag) {
                                if (offsetY.value <= -maxDragUpPx * 0.8f) {
                                    onClick() 
                                } else if (offsetY.value >= maxDragDownPx * 0.6f) {
                                    vibrateClick(context)
                                    launch { 
                                        offsetY.animateTo(500f, tween(300))
                                        viewModel.stopAndClearQueue()
                                        offsetY.snapTo(0f)
                                    }
                                    return@launch
                                }
                            } else if (isHorizontalDrag && !isRadioMode) {
                                if (offsetX.value > 150f) {
                                    viewModel.skipToPrevious()
                                    vibrateClick(context)
                                } else if (offsetX.value < -150f) {
                                    viewModel.skipToNext()
                                    vibrateClick(context)
                                }
                            }
                            
                            launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                            launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f, spring())
                            offsetY.animateTo(0f, spring())
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        
                        if (!isVerticalDrag && !isHorizontalDrag) {
                            if (abs(dragAmount.y) > abs(dragAmount.x)) {
                                isVerticalDrag = true
                            } else {
                                isHorizontalDrag = true
                            }
                        }

                        scope.launch {
                            if (isVerticalDrag) {
                                val newY = (offsetY.value + dragAmount.y).coerceIn(-maxDragUpPx, maxDragDownPx)
                                
                                if ((newY <= -maxDragUpPx || newY >= maxDragDownPx * 0.8f) && !hasVibratedOnLimit) {
                                    vibrateClick(context)
                                    hasVibratedOnLimit = true
                                } else if (newY > -maxDragUpPx && newY < maxDragDownPx * 0.8f) {
                                    hasVibratedOnLimit = false
                                }

                                offsetY.snapTo(newY)
                            } else if (isHorizontalDrag && !isRadioMode) {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                            }
                        }
                    }
                )
            }
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                scaleX = scale
                scaleY = scale
                if (offsetY.value > 0) {
                    alpha = (1f - (offsetY.value / maxDragDownPx)).coerceIn(0f, 1f)
                }
            }
            .clip(RoundedCornerShape(dynamicCornerRadius))
            .bouncingClickable(
                enabled = isInteractionEnabled,
                onClick = onClick
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(dynamicCornerRadius),
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
                    with(sharedTransitionScope) {
                        AlbumArt(
                            albumArtUri = currentTrack?.albumArtUri,
                            modifier = Modifier
                                .size(48.dp)
                                .sharedElement(
                                    rememberSharedContentState(key = "album_art_${currentTrack?.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                            shape = RoundedCornerShape(8.dp),
                            updateTrigger = currentTrack?.dateModified ?: 0L
                        )
                    }
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
                    },
                    enabled = isInteractionEnabled
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isInteractionEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
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
