package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import kotlinx.coroutines.launch

@Composable
fun TracksScreen(
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit
) {
    var isFavoritesMode by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    val tracks by if (isFavoritesMode) {
        trackViewModel.favoriteTracks.collectAsState()
    } else {
        trackViewModel.filteredTracks.collectAsState()
    }
    
    val searchQuery by trackViewModel.searchQuery.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (searchQuery.isEmpty()) {
                    if (isFavoritesMode) Text("No favorites yet") else CircularProgressIndicator()
                } else {
                    Text("Nothing found")
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 70.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tracks) { index, track ->
                        TrackCard(
                            track = track,
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = isPlaying,
                            onClick = {
                                playerViewModel.playTracks(tracks, index)
                                onNavigateToPlayer()
                            }
                        )
                    }
                }

                FastScrollbar(
                    listState = listState,
                    itemCount = tracks.size,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(top = 80.dp, bottom = 180.dp, end = 4.dp)
                        .width(20.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .padding(top = 16.dp, end = 16.dp)
                .align(Alignment.TopEnd),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
            tonalElevation = 8.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeToggleButton(
                    selected = !isFavoritesMode,
                    icon = Icons.Default.MusicNote,
                    onClick = { isFavoritesMode = false }
                )
                
                ModeToggleButton(
                    selected = isFavoritesMode,
                    icon = Icons.Default.Favorite,
                    onClick = { isFavoritesMode = true }
                )
            }
        }
    }
}

@Composable
fun FastScrollbar(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    val scrollbarAlpha by animateFloatAsState(
        targetValue = if (listState.isScrollInProgress) 1f else 0.3f,
        label = "alpha"
    )

    BoxWithConstraints(modifier = modifier) {
        val maxHeight = constraints.maxHeight.toFloat()
        
        val firstVisibleIndex = listState.firstVisibleItemIndex
        val layoutInfo = listState.layoutInfo
        val visibleItemsCount = layoutInfo.visibleItemsInfo.size
        
        if (itemCount > visibleItemsCount && visibleItemsCount > 0) {
            val thumbHeight = (visibleItemsCount.toFloat() / itemCount) * maxHeight
            val scrollPercent = firstVisibleIndex.toFloat() / (itemCount - visibleItemsCount).coerceAtLeast(1)
            val thumbOffset = scrollPercent * (maxHeight - thumbHeight)

            Box(
                modifier = Modifier
                    .offset(y = (thumbOffset / LocalContext.current.resources.displayMetrics.density).dp)
                    .width(4.dp)
                    .height((thumbHeight / LocalContext.current.resources.displayMetrics.density).dp)
                    .align(Alignment.TopCenter)
                    .alpha(scrollbarAlpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            val scrollFactor = itemCount.toFloat() / maxHeight
                            scope.launch {
                                listState.scrollToItem(
                                    (listState.firstVisibleItemIndex + (delta * scrollFactor).toInt())
                                        .coerceIn(0, (itemCount - 1).coerceAtLeast(0))
                                )
                            }
                        }
                    )
            )
        }
    }
}

@Composable
fun ModeToggleButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "bg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "icon"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
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

@Composable
fun TrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(track.albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isCurrent) {
                PlayingEqualizer(
                    isPlaying = isPlaying,
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                )
            }
        }
    }
}
