package com.nkds.hosikoouma.jasmine.ui.components

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackInfoBottomSheet(
    track: Track,
    onDismissRequest: () -> Unit,
    trackViewModel: TrackViewModel = viewModel()
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }

    // Проверяем, локальный ли это файл и можно ли его редактировать
    val isEditable = remember(track) {
        track.path.isNotEmpty() && File(track.path).exists() && track.contentUri.scheme != "telegram"
    }

    val trackDetails = remember(track) {
        val details = mutableMapOf<String, String>()
        
        if (track.path.isNotEmpty()) {
            val file = File(track.path)
            if (file.exists()) {
                try {
                    val audioFile = AudioFileIO.read(file)
                    val header = audioFile.audioHeader
                    
                    details["Format"] = header.encodingType ?: file.extension.uppercase()
                    details["Bitrate"] = "${header.bitRate} kbps"
                    details["Sample Rate"] = "${header.sampleRate} Hz"
                    details["Channels"] = header.channels
                    details["Size"] = "%.2f MB".format(file.length() / (1024f * 1024f))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        if (details.isEmpty() || details["Format"] == "Unknown") {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, track.contentUri)
                val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                val sampleRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                
                if (details["Bitrate"] == null && bitrate != null) {
                    details["Bitrate"] = "${bitrate.toInt() / 1000} kbps"
                }
                if (details["Format"] == null || details["Format"] == "Unknown") {
                    details["Format"] = mimeType?.substringAfter("/")?.uppercase() ?: "Unknown"
                }
                if (details["Sample Rate"] == null && sampleRateStr != null) {
                    details["Sample Rate"] = "$sampleRateStr Hz"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
        details
    }

    if (showEditDialog) {
        TrackEditDialog(
            track = track,
            onDismiss = { showEditDialog = false },
            onConfirm = { title, artist, album, cover ->
                trackViewModel.updateTrackMetadata(track, title, artist, album, cover)
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Track Info",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (isEditable) {
                    FilledTonalIconButton(
                        onClick = { showEditDialog = true },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoItem(label = "Title", value = track.title)
            InfoItem(label = "Artist", value = track.artist)
            InfoItem(label = "Album", value = track.album)
            InfoItem(label = "Duration", value = formatTime(track.duration))
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            Text(
                text = "Technical Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            InfoItem(label = "Format / Codec", value = trackDetails["Format"] ?: "Unknown")
            InfoItem(label = "Sample Rate", value = trackDetails["Sample Rate"] ?: "Unknown")
            InfoItem(label = "Bitrate", value = trackDetails["Bitrate"] ?: "Unknown")
            
            if (trackDetails.containsKey("Channels")) {
                InfoItem(label = "Channels", value = trackDetails["Channels"]!!)
            }
            if (trackDetails.containsKey("Size")) {
                InfoItem(label = "File Size", value = trackDetails["Size"]!!)
            }
            
            InfoItem(label = "File Location", value = track.path.ifEmpty { "System Media Store (URI: ${track.contentUri})" })
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
