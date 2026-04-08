package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nkds.hosikoouma.jasmine.datamodels.Track

@Composable
fun TrackCard(
    track: Track,
    isCurrent: Boolean,
    isManual: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    val cardColor by animateColorAsState(
        targetValue = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(500),
        label = "cardColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isManual && !isCurrent) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(
                albumArtUri = track.albumArtUri,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isManual && !isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isCurrent) {
                PlayingEqualizer(
                    isPlaying = isPlaying,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            trailingContent()
        }
    }
}

@Composable
fun PlayingEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    
    @Composable
    fun animateBar(initial: Float, target: Float, duration: Int): State<Float> {
        return infiniteTransition.animateFloat(
            initialValue = initial,
            targetValue = target,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar"
        )
    }

    val bar1 = if (isPlaying) animateBar(0.2f, 0.8f, 400) else remember { mutableStateOf(0.3f) }
    val bar2 = if (isPlaying) animateBar(0.3f, 1.0f, 500) else remember { mutableStateOf(0.5f) }
    val bar3 = if (isPlaying) animateBar(0.2f, 0.7f, 350) else remember { mutableStateOf(0.4f) }

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(bar1, bar2, bar3).forEach { heightState ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightState.value)
                    .background(color, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
            )
        }
    }
}
