package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch
import java.util.Locale

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
    
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Анимированное смещение.
    val animatedOffset = remember { Animatable(1000f) }

    // Анимация появления при входе (сделаем её чуть жестче для скорости)
    LaunchedEffect(Unit) {
        animatedOffset.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    // Slider logic
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(progress) {
        val now = System.currentTimeMillis()
        if (!isPressed && (now - lastSeekTime > 1000L)) {
            sliderValue = progress.toFloat()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = animatedOffset.value.coerceAtLeast(0f)
                alpha = (1f - (animatedOffset.value / 2500f)).coerceIn(0f, 1f)
            }
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (animatedOffset.value > 300) {
                            // Вызываем закрытие не дожидаясь конца анимации,
                            // но запускаем "долет" вниз для визуальной инерции
                            onClose()
                            scope.launch {
                                animatedOffset.animateTo(
                                    targetValue = 1500f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                )
                            }
                        } else {
                            scope.launch {
                                animatedOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            animatedOffset.snapTo(animatedOffset.value + dragAmount)
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)),
                color = Color.DarkGray,
                tonalElevation = 8.dp
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentTrack?.albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTrack?.title ?: "Unknown Title",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentTrack?.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
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
                    interactionSource = interactionSource,
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(sliderValue.toLong()), color = Color.Gray, fontSize = 12.sp)
                    Text(formatTime(duration), color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ОБНОВЛЕННАЯ ЛОГИКА ПОВТОРА
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.toggleRepeatMode() }) {
                    val repeatIcon = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    val repeatColor = when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> Color.White
                        else -> MaterialTheme.colorScheme.primary
                    }
                    
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = repeatColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.skipToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(44.dp))
                }
                
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.skipToNext() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(44.dp))
                }

                Surface(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.togglePlayPause() },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(64.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = "Queue", tint = Color.Gray, modifier = Modifier.size(26.dp))
                }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.toggleFavoriteCurrent() }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                    Icon(Icons.Default.Lyrics, contentDescription = "Lyrics", tint = Color.Gray, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.Gray, modifier = Modifier.size(26.dp))
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
