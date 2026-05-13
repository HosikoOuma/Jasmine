package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkds.hosikoouma.jasmine.R
import com.nkds.hosikoouma.jasmine.datamodels.LyricsLine
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.TrackCard
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel

// --- UI State ---
data class LyricsUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val progress: Long = 0,
    val localLyrics: String? = null,
    val remotePlainLyrics: String? = null,
    val syncedLocal: List<LyricsLine>? = null,
    val syncedRemote: List<LyricsLine>? = null,
    val isLoading: Boolean = false
)

// --- Stateful Screen ---
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LyricsScreen(
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val localLyrics by viewModel.localLyrics.collectAsStateWithLifecycle()
    val remoteLyrics by viewModel.remoteLyrics.collectAsStateWithLifecycle()
    val syncedLocal by viewModel.syncedLocalLyrics.collectAsStateWithLifecycle()
    val syncedRemote by viewModel.syncedRemoteLyrics.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingLyrics.collectAsStateWithLifecycle()

    // Запускаем загрузку текста только при открытии экрана
    LaunchedEffect(currentTrack) {
        if (currentTrack != null) {
            viewModel.loadLyricsForCurrentTrack()
        }
    }

    val uiState = LyricsUiState(
        currentTrack = currentTrack,
        isPlaying = isPlaying,
        progress = progress,
        localLyrics = localLyrics,
        remotePlainLyrics = remoteLyrics?.plainLyrics,
        syncedLocal = syncedLocal,
        syncedRemote = syncedRemote,
        isLoading = isLoading
    )

    LyricsContent(
        uiState = uiState,
        onClose = onClose,
        onSeek = viewModel::seekTo,

    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun LyricsContent(
    uiState: LyricsUiState,
    onClose: () -> Unit,
    onSeek: (Long) -> Unit,

) {
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
            LyricsHeader(isLrcLibMode, onModeChange = { isLrcLibMode = it })

            // Track Section
            uiState.currentTrack?.let { track ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TrackCard(
                            track = track,
                            isCurrent = true,
                            isPlaying = uiState.isPlaying,
                            onClick = {},

                        )

                }
            }

            // Divider
            LyricsDivider()

            // Lyrics Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val currentSynced = if (isLrcLibMode) uiState.syncedRemote else uiState.syncedLocal
                    val currentPlain = if (isLrcLibMode) uiState.remotePlainLyrics else uiState.localLyrics

                    when {
                        currentSynced != null -> {
                            SyncedLyricsView(currentSynced, uiState.progress, haptic, onSeek)
                        }
                        currentPlain != null -> {
                            PlainLyricsView(currentPlain)
                        }
                        else -> {
                            val msg = if (isLrcLibMode) stringResource(R.string.no_lyrics_lrclib) else stringResource(R.string.no_lyrics_local)
                            Text(msg, color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }
}

// --- Internal Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsHeader(isLrcLibMode: Boolean, onModeChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.lyrics),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = !isLrcLibMode,
                onClick = { onModeChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(stringResource(R.string.lyrics_local)) }
            SegmentedButton(
                selected = isLrcLibMode,
                onClick = { onModeChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(stringResource(R.string.lyrics_lrclib)) }
        }
    }
}

@Composable
private fun LyricsDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
fun SyncedLyricsView(
    lines: List<LyricsLine>,
    currentProgress: Long,
    haptic: HapticFeedback,
    onSeek: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val currentLineIndex = lines.indexOfLast { it.timestamp <= currentProgress }

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            listState.animateScrollToItem(index = currentLineIndex, scrollOffset = -400)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lines) { index, line ->
            val isCurrent = index == currentLineIndex
            LyricsLineItem(line, isCurrent, haptic, onSeek)
        }
        item { Spacer(modifier = Modifier.height(400.dp)) }
    }
}

@Composable
fun LyricsLineItem(
    line: LyricsLine,
    isCurrent: Boolean,
    haptic: HapticFeedback,
    onSeek: (Long) -> Unit
) {
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

@Composable
fun InstrumentalAnimation(active: Boolean, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "notes")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(Icons.Default.MusicNote, null, tint = color, modifier = Modifier.size(32.dp).rotate(if (active) rotation else 0f))
        Icon(Icons.Default.MusicNote, null, tint = color, modifier = Modifier.size(32.dp).scale(if (active) pulseScale else 1f))
    }
}

@Composable
fun PlainLyricsView(lyrics: String) {
    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            text = lyrics,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 32.sp, letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 100.dp)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true)
@Composable
fun LyricsPreview() {
    val sampleTrack = Track(1, "Luminous", "Jasmine", "Garden", 180000, android.net.Uri.EMPTY, null)
    JasmineTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                LyricsContent(
                    uiState = LyricsUiState(
                        currentTrack = sampleTrack,
                        isPlaying = true,
                        progress = 5000,
                        syncedLocal = listOf(
                            LyricsLine(0, "Welcome to the garden"),
                            LyricsLine(5000, "Where the flowers bloom"),
                            LyricsLine(10000, "In the moonlight")
                        )
                    ),
                    onClose = {},
                    onSeek = {},

                )
            }
        }
    }
}
