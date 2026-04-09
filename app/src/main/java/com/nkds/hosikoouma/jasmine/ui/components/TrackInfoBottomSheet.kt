package com.nkds.hosikoouma.jasmine.ui.components

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nkds.hosikoouma.jasmine.datamodels.Track
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackInfoBottomSheet(
    track: Track,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val trackDetails = remember(track) {
        val details = mutableMapOf<String, String>()
        
        // 1. Попытка получить расширенные данные через JAudioTagger
        if (track.path.isNotEmpty()) {
            val file = File(track.path)
            if (file.exists()) {
                try {
                    val audioFile = AudioFileIO.read(file)
                    val header = audioFile.audioHeader
                    
                    // Формат (кодек)
                    details["Format"] = header.encodingType ?: file.extension.uppercase()
                    // Битрейт
                    details["Bitrate"] = "${header.bitRate} kbps"
                    // Частота дискретизации
                    details["Sample Rate"] = "${header.sampleRate} Hz"
                    // Каналы
                    details["Channels"] = header.channels
                    // Размер файла
                    details["Size"] = "%.2f MB".format(file.length() / (1024f * 1024f))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // 2. Резервный вариант через MediaMetadataRetriever (если JAudioTagger не смог или данных нет)
        if (details.isEmpty() || details["Format"] == "Unknown") {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, track.contentUri)
                val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                val sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE) // API 29+
                
                if (details["Bitrate"] == null && bitrate != null) {
                    details["Bitrate"] = "${bitrate.toInt() / 1000} kbps"
                }
                if (details["Format"] == null || details["Format"] == "Unknown") {
                    details["Format"] = mimeType?.substringAfter("/")?.uppercase() ?: "Unknown"
                }
                if (details["Sample Rate"] == null && sampleRate != null) {
                    details["Sample Rate"] = "$sampleRate Hz"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
        details
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Track Technical Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            InfoItem(label = "Title", value = track.title)
            InfoItem(label = "Artist", value = track.artist)
            InfoItem(label = "Duration", value = formatTime(track.duration))
            
            InfoItem(label = "Format / Codec", value = trackDetails["Format"] ?: "Unknown")
            InfoItem(label = "Sample Rate", value = trackDetails["Sample Rate"] ?: "Unknown")
            InfoItem(label = "Bitrate", value = trackDetails["Bitrate"] ?: "Unknown")
            
            if (trackDetails.containsKey("Channels")) {
                InfoItem(label = "Channels", value = trackDetails["Channels"]!!)
            }
            if (trackDetails.containsKey("Size")) {
                InfoItem(label = "File Size", value = trackDetails["Size"]!!)
            }
            
            // Показываем полный физический путь
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
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
