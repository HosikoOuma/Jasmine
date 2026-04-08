package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkds.hosikoouma.jasmine.ui.components.TrackCard
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    
    val localLyrics by viewModel.localLyrics.collectAsState()
    val remoteLyrics by viewModel.remoteLyrics.collectAsState()
    
    val syncedLocal by viewModel.syncedLocalLyrics.collectAsState()
    val syncedRemote by viewModel.syncedRemoteLyrics.collectAsState()
    
    val isLoading by viewModel.isLoadingLyrics.collectAsState()
    
    var isLrcLibMode by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = !isLrcLibMode,
                        onClick = { isLrcLibMode = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Local") }
                    SegmentedButton(
                        selected = isLrcLibMode,
                        onClick = { isLrcLibMode = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("LRCLIB") }
                }
            }

            // Track Card
            currentTrack?.let { track ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TrackCard(
                        track = track,
                        isCurrent = true,
                        isPlaying = isPlaying,
                        onClick = {}
                    )
                }
            }

            // Разделитель
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            // Область контента
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    if (isLrcLibMode) {
                        // РЕЖИМ LRCLIB
                        val lyricsObj = remoteLyrics
                        if (syncedRemote != null) {
                            SyncedLyricsView(syncedRemote!!, progress, haptic) { viewModel.seekTo(it) }
                        } else if (lyricsObj?.plainLyrics != null) {
                            PlainLyricsView(lyricsObj.plainLyrics)
                        } else {
                            Text("No lyrics found on LRCLIB.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                        }
                    } else {
                        // ЛОКАЛЬНЫЙ РЕЖИМ
                        if (syncedLocal != null) {
                            SyncedLyricsView(syncedLocal!!, progress, haptic) { viewModel.seekTo(it) }
                        } else if (localLyrics != null) {
                            PlainLyricsView(localLyrics!!)
                        } else {
                            Text("No local lyrics found in tags or .lrc file.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncedLyricsView(
    lines: List<com.nkds.hosikoouma.jasmine.datamodels.LyricsLine>,
    currentProgress: Long,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onSeek: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val currentLineIndex = lines.indexOfLast { it.timestamp <= currentProgress }

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            listState.animateScrollToItem(currentLineIndex, scrollOffset = -300)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lines) { index, line ->
            val isCurrent = index == currentLineIndex
            val color by animateColorAsState(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            val scale by animateFloatAsState(if (isCurrent) 1.05f else 1f)
            
            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 22.sp,
                    lineHeight = 32.sp
                ),
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .scale(scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSeek(line.timestamp)
                    }
            )
        }
        item { Spacer(modifier = Modifier.height(200.dp)) }
    }
}

@Composable
fun PlainLyricsView(lyrics: String) {
    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            text = lyrics,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 32.sp,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 100.dp)
        )
    }
}
