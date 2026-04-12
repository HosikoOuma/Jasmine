package com.nkds.hosikoouma.jasmine

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.kmpalette.rememberDominantColorState
import com.nkds.hosikoouma.jasmine.ui.screens.ExternalPlayerScreen
import com.nkds.hosikoouma.jasmine.ui.theme.*
import com.nkds.hosikoouma.jasmine.viewmodels.DarkMode
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel

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

        // Инициализируем плеер
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            
            val fontStyle by settingsViewModel.appFontFamily.collectAsState()
            val darkModeSetting by settingsViewModel.darkMode.collectAsState()
            val amoledMode by settingsViewModel.amoledDarkMode.collectAsState()
            val useDynamicColor by settingsViewModel.useDynamicColor.collectAsState()
            val paletteStyle by settingsViewModel.paletteStyle.collectAsState()
            val useAlbumArtColor by settingsViewModel.useAlbumArtColor.collectAsState()
            val savedSeedColorInt by settingsViewModel.seedColor.collectAsState()

            val darkTheme = when (darkModeSetting) {
                DarkMode.DARK.name -> true
                DarkMode.LIGHT.name -> false
                else -> isSystemInDarkTheme()
            }

            // Получаем метаданные внешнего файла
            val meta = remember(uri) { getExternalMetadata(uri) }
            
            val dominantColorState = rememberDominantColorState()

            LaunchedEffect(meta.artwork, useAlbumArtColor) {
                if (useAlbumArtColor && meta.artwork != null) {
                    try {
                        val bitmap = BitmapFactory.decodeByteArray(meta.artwork, 0, meta.artwork.size)
                        if (bitmap != null) {
                            dominantColorState.updateFrom(bitmap.asImageBitmap())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val seedColor = if (useAlbumArtColor && meta.artwork != null && dominantColorState.color != Color.Unspecified) {
                dominantColorState.color
            } else {
                Color(savedSeedColorInt)
            }

            val currentFontFamily = when(fontStyle) {
                "GOOGLE_SANS" -> GoogleSans
                "JETBRAINS_MONO" -> JetBrainsMonoNerd
                "NUNITO" -> Nunito
                else -> androidx.compose.ui.text.font.FontFamily.Default
            }
            
            val currentTypography = getTypography(currentFontFamily)

            JasmineTheme(
                darkTheme = darkTheme,
                amoledMode = amoledMode,
                useDynamicColor = useDynamicColor,
                seedColor = seedColor,
                paletteStyle = paletteStyle,
                typography = currentTypography
            ) {
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
