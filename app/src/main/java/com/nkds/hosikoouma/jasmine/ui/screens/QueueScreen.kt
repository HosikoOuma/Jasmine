package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.ui.components.TrackCard
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun QueueScreen(
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    val playlist by viewModel.playlist.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    
    val currentIndex = remember(playlist, currentTrack) {
        playlist.indexOfFirst { it.uid == currentTrack?.uid }
    }
    
    val upNextPlaylist = remember(playlist, currentIndex) {
        if (currentIndex != -1 && currentIndex < playlist.size - 1) {
            playlist.subList(currentIndex + 1, playlist.size)
        } else {
            emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playing Queue",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClose) {
                    Text("Done", color = MaterialTheme.colorScheme.primary)
                }
            }

            currentTrack?.let { track ->
                Text(
                    text = "Now Playing",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TrackCard(
                        track = track,
                        isCurrent = true,
                        isPlaying = isPlaying,
                        onClick = {}
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            Text(
                text = "Up Next",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )

            val lazyListState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val actualFrom = from.index + currentIndex + 1
                val actualTo = to.index + currentIndex + 1
                viewModel.moveTrack(actualFrom, actualTo)
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(upNextPlaylist, key = { _, track -> track.uid }) { index, track ->
                    ReorderableItem(reorderableState, key = track.uid) { isDragging ->
                        val elevation = if (isDragging) 8.dp else 0.dp
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { 
                                    shadowElevation = elevation.toPx()
                                }
                        ) {
                            SwipeableTrackCard(
                                track = track,
                                isCurrent = false,
                                isPlaying = false,
                                isManualMarkingEnabled = true,
                                onSwipeAction = { viewModel.removeFromQueue(track) }, // ТЕПЕРЬ УДАЛЯЕТ
                                onClick = { viewModel.skipToQueueItem(index + currentIndex + 1) },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Reorder",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .draggableHandle()
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
