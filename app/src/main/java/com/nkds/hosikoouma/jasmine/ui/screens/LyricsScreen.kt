package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        val lyricsObj = remoteLyrics
                        if (syncedRemote != null) {
                            SyncedLyricsView(syncedRemote!!, progress, haptic) { viewModel.seekTo(it) }
                        } else if (lyricsObj?.plainLyrics != null) {
                            PlainLyricsView(lyricsObj.plainLyrics)
                        } else {
                            Text("No lyrics found on LRCLIB.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                        }
                    } else {
                        if (syncedLocal != null) {
                            SyncedLyricsView(syncedLocal!!, progress, haptic) { viewModel.seekTo(it) }
                        } else if (localLyrics != null) {
                            PlainLyricsView(localLyrics!!)
                        } else {
                            Text("No local lyrics found.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
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
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            listState.animateScrollToItem(
                index = currentLineIndex,
                scrollOffset = -400
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lines) { index, line ->
            val isCurrent = index == currentLineIndex
            val color by animateColorAsState(
                targetValue = if (isCurrent) MaterialTheme.colorScheme.primary 
                              else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                animationSpec = tween(400),
                label = "color"
            )
            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1.0f else 0.9f,
                animationSpec = tween(400),
                label = "scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isCurrent) 1f else 0.4f,
                animationSpec = tween(400),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSeek(line.timestamp)
                    },
                contentAlignment = Alignment.Center
            ) {
                // Если строка пустая или содержит "инструментал" - показываем анимацию нот
                if (line.text.isBlank() || line.text.contains("instrumental", ignoreCase = true) || line.text == "♪") {
                    InstrumentalAnimation(isCurrent, color)
                } else {
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            lineHeight = 38.sp
                        ),
                        color = color,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(400.dp)) }
    }
}

@Composable
fun InstrumentalAnimation(active: Boolean, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "notes")
    
    // Нота 1: Вращается
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Нота 2: Пульсирует (меняет размер)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(32.dp)
                .rotate(if (active) rotation else 0f)
        )
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(32.dp)
                .scale(if (active) pulseScale else 1f)
        )
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
