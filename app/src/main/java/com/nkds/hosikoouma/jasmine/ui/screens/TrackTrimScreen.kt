package com.nkds.hosikoouma.jasmine.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.nkds.hosikoouma.jasmine.R
import com.nkds.hosikoouma.jasmine.viewmodels.TrimViewModel

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun TrackTrimScreen(
    trackId: Long,
    onBack: () -> Unit,
    viewModel: TrimViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val track by viewModel.track.collectAsState()
    val waveform by viewModel.waveform.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val endTime by viewModel.endTime.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()

    LaunchedEffect(trackId) {
        viewModel.loadTrack(trackId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.trim_track), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                }
            },
            actions = {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = {
                        viewModel.export(
                            onSuccess = {
                                Toast.makeText(context, R.string.trimming_success, Toast.LENGTH_SHORT).show()
                                onBack()
                            },
                            onError = { error ->
                                Toast.makeText(context, "${context.getString(R.string.trimming_failed)}: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                    }) {
                        Icon(Icons.Rounded.Save, null)
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 150.dp), // Отступы под миниплеер и навбар
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            track?.let {
                Text(it.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(it.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(modifier = Modifier.height(48.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    if (waveform.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        WaveformView(
                            waveform = waveform,
                            startTime = startTime,
                            endTime = endTime,
                            duration = it.duration,
                            currentPosition = currentPosition,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.start_time, formatTime(startTime)), style = MaterialTheme.typography.labelMedium)
                    Text(stringResource(R.string.end_time, formatTime(endTime)), style = MaterialTheme.typography.labelMedium)
                }

                RangeSlider(
                    value = startTime.toFloat()..endTime.toFloat(),
                    onValueChange = { range ->
                        viewModel.setStartTime(range.start.toLong())
                        viewModel.setEndTime(range.endInclusive.toLong())
                    },
                    valueRange = 0f..it.duration.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(48.dp))

                FilledIconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WaveformView(
    waveform: FloatArray,
    startTime: Long,
    endTime: Long,
    duration: Long,
    currentPosition: Long,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val playbackColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier.padding(vertical = 16.dp)) {
        val width = size.width
        val height = size.height
        val barWidth = width / waveform.size
        
        val startX = (startTime.toFloat() / duration.toFloat()) * width
        val endX = (endTime.toFloat() / duration.toFloat()) * width
        val playX = (currentPosition.toFloat() / duration.toFloat()) * width

        waveform.forEachIndexed { index, peak ->
            val x = index * barWidth
            val barHeight = peak * height
            val color = if (x in startX..endX) primaryColor else secondaryColor
            
            drawRect(
                color = color,
                topLeft = Offset(x + 1.dp.toPx(), (height - barHeight) / 2),
                size = Size(barWidth - 2.dp.toPx(), barHeight)
            )
        }

        // Playback indicator
        drawLine(
            color = playbackColor,
            start = Offset(playX, 0f),
            end = Offset(playX, height),
            strokeWidth = 2.dp.toPx()
        )
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
