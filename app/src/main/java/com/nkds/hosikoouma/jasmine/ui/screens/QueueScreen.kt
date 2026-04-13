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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.ui.components.TrackCard
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// --- UI State ---
data class QueueUiState(
    val playlist: List<Track> = emptyList(),
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false
)

@Composable
fun QueueScreen(
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    QueueContent(
        uiState = QueueUiState(playlist, currentTrack, isPlaying),
        onClose = onClose,
        onMoveTrack = viewModel::moveTrack,
        onRemoveFromQueue = viewModel::removeFromQueue,
        onAddToQueue = { viewModel.addToQueue(it, showToast = true) },
        onSkipToItem = viewModel::skipToQueueItem
    )
}

@Composable
fun QueueContent(
    uiState: QueueUiState,
    onClose: () -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    onRemoveFromQueue: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onSkipToItem: (Int) -> Unit
) {
    val currentIndex = remember(uiState.playlist, uiState.currentTrack) {
        uiState.playlist.indexOfFirst { it.uid == uiState.currentTrack?.uid }
    }
    
    // Локальный список для мгновенного перемещения без лагов
    var localUpNext by remember(uiState.playlist, currentIndex) {
        mutableStateOf(
            if (currentIndex != -1 && currentIndex < uiState.playlist.size - 1) {
                uiState.playlist.subList(currentIndex + 1, uiState.playlist.size)
            } else emptyList()
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            QueueHeader(onClose)

            uiState.currentTrack?.let { track ->
                SectionLabel("Now Playing", MaterialTheme.colorScheme.primary)
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TrackCard(track = track, isCurrent = true, isPlaying = uiState.isPlaying, onClick = {})
                }
            }

            QueueDivider()
            SectionLabel("Up Next", MaterialTheme.colorScheme.secondary)

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
                                    // Исправленная логика: удаление если ручной, добавление если нет
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
            text = "Playing Queue", 
            style = MaterialTheme.typography.titleLarge, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface // Явно задаем цвет
        )
        TextButton(onClick = onClose) { 
            Text("Done", color = MaterialTheme.colorScheme.primary) 
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
            uiState = QueueUiState(), 
            onClose = {}, 
            onMoveTrack = { _, _ -> }, 
            onRemoveFromQueue = {}, 
            onAddToQueue = {}, 
            onSkipToItem = {}
        )
    }
}
