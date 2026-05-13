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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkds.hosikoouma.jasmine.R
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.ui.components.TrackCard
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun QueueScreen(
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    QueueContent(
        playlist = playlist,
        currentTrack = currentTrack,
        isPlaying = isPlaying,
        onClose = onClose,
        onMoveTrack = viewModel::moveTrack,
        onRemoveFromQueue = viewModel::removeFromQueue,
        onAddToQueue = { viewModel.addToQueue(it, showToast = true) },
        onSkipToItem = viewModel::skipToQueueItem
    )
}

@Composable
fun QueueContent(
    playlist: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onClose: () -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    onRemoveFromQueue: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onSkipToItem: (Int) -> Unit
) {
    val currentIndex by remember(playlist, currentTrack) {
        derivedStateOf { playlist.indexOfFirst { it.uid == currentTrack?.uid } }
    }
    
    // Локальный список для мгновенного перемещения без лагов
    var localUpNext by remember(playlist, currentIndex) {
        mutableStateOf(
            if (currentIndex != -1 && currentIndex < playlist.size - 1) {
                playlist.subList(currentIndex + 1, playlist.size)
            } else if (currentIndex == -1 && playlist.isNotEmpty()) {
                playlist // Если текущий трек не найден в списке (например, радио), показываем всё
            } else emptyList()
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            QueueHeader(onClose)

            currentTrack?.let { track ->
                SectionLabel(stringResource(R.string.now_playing), MaterialTheme.colorScheme.primary)
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TrackCard(track = track, isCurrent = true, isPlaying = isPlaying, onClick = {})
                }
            }

            QueueDivider()
            SectionLabel(stringResource(R.string.up_next), MaterialTheme.colorScheme.secondary)

            val lazyListState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                localUpNext = localUpNext.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
                val actualFrom = from.index + currentIndex + 1
                val actualTo = to.index + currentIndex + 1
                onMoveTrack(actualFrom, actualTo)
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(localUpNext, key = { _, track -> track.uid }) { index, track ->
                    ReorderableItem(reorderableState, key = track.uid) { isDragging ->
                        val elevation = if (isDragging) 12.dp else 0.dp
                        Box(modifier = Modifier.fillMaxWidth().graphicsLayer { shadowElevation = elevation.toPx() }) {
                            SwipeableTrackCard(
                                track = track,
                                isCurrent = false,
                                isPlaying = false,
                                isManualMarkingEnabled = true,
                                enabled = true,
                                onSwipeAction = { 
                                    if (track.isManual) onRemoveFromQueue(track)
                                    else onAddToQueue(track)
                                },
                                onClick = { onSkipToItem(index + currentIndex + 1) },
                                trailingContent = {
                                    Icon(
                                        Icons.Default.DragHandle, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(28.dp).draggableHandle()
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

@Composable
private fun QueueHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp), 
        horizontalArrangement = Arrangement.SpaceBetween, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.playing_queue), 
            style = MaterialTheme.typography.titleLarge, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = onClose) { 
            Text(stringResource(R.string.done), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
}

@Composable
private fun QueueDivider() {
    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp).fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
}

@Preview(showBackground = true)
@Composable
fun QueuePreview() {
    JasmineTheme {
        QueueContent(
            playlist = emptyList(),
            currentTrack = null,
            isPlaying = false,
            onClose = {},
            onMoveTrack = { _, _ -> },
            onRemoveFromQueue = {},
            onAddToQueue = {},
            onSkipToItem = {}
        )
    }
}
