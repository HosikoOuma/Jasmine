package com.nkds.hosikoouma.jasmine

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.nkds.hosikoouma.jasmine.ui.components.JasmineThemeWrapper
import com.nkds.hosikoouma.jasmine.ui.screens.ExternalPlayerScreen

class ExternalPlayerActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val uri: Uri? = intent.data
        if (uri == null) {
            finish()
            return
        }

        // Инициализация плеера
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }

        setContent {
            // Получаем метаданные один раз
            val meta = remember(uri) { getExternalMetadata(uri) }

            JasmineThemeWrapper(albumArtBytes = meta.artwork) {
                ExternalPlayerScreen(
                    player = exoPlayer!!,
                    title = meta.title,
                    artist = meta.artist,
                    artwork = meta.artwork,
                    onClose = { finish() }
                )
            }
        }
    }

    private data class ExternalMeta(val title: String, val artist: String, val artwork: ByteArray?)

    private fun getExternalMetadata(uri: Uri): ExternalMeta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: uri.lastPathSegment ?: "Unknown Title"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val artwork = retriever.embeddedPicture
            ExternalMeta(title, artist, artwork)
        } catch (e: Exception) {
            ExternalMeta(uri.lastPathSegment ?: "Unknown", "Unknown Artist", null)
        } finally {
            retriever.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}
