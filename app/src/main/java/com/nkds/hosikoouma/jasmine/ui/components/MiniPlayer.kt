package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel

@Composable
fun MiniPlayer(
    viewModel: PlayerViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()

    if (currentTrack == null) return

    val progressFactor by animateFloatAsState(
        targetValue = if (duration > 0) progress.toFloat() / duration.toFloat() else 0f,
        label = "progress"
    )

    // АНИМАЦИЯ "ЖИВОГО" ПЛЕЕРА (плавное покачивание вверх-вниз)
    val infiniteTransition = rememberInfiniteTransition(label = "living_player")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    // ЭФФЕКТ НАЖАТИЯ (уменьшение масштаба)
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
            .graphicsLayer {
                translationY = floatingOffset
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Убираем стандартный ripple, так как у нас есть масштаб
                onClick = onClick
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ФОНОВЫЙ ПРОГРЕСС-БАР
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressFactor)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentTrack?.albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentTrack?.title ?: "",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentTrack?.artist ?: "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { viewModel.togglePlayPause() }
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
