package com.nkds.hosikoouma.jasmine.ui.components

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmpalette.rememberDominantColorState
import com.nkds.hosikoouma.jasmine.core.models.AppFontFamily
import com.nkds.hosikoouma.jasmine.core.models.DarkMode
import com.nkds.hosikoouma.jasmine.ui.theme.*
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun JasmineThemeWrapper(
    albumArtUri: Uri? = null,
    albumArtBytes: ByteArray? = null,
    settingsViewModel: SettingsViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 1. Dark Mode Logic
    val darkTheme = when (settings.darkMode) {
        DarkMode.DARK -> true
        DarkMode.LIGHT -> false
        DarkMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }

    // 2. Dynamic Color Extraction
    val dominantColorState = rememberDominantColorState()
    
    LaunchedEffect(albumArtUri, albumArtBytes, settings.useAlbumArtColor) {
        if (settings.useAlbumArtColor) {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    when {
                        albumArtBytes != null -> {
                            // Для байтов используем меньший коэффициент сжатия для лучшей точности цвета
                            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                            BitmapFactory.decodeByteArray(albumArtBytes, 0, albumArtBytes.size, options)
                        }
                        albumArtUri != null -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val source = ImageDecoder.createSource(context.contentResolver, albumArtUri)
                                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                                    // Увеличиваем целевой размер до 256px для более точного анализа палитры
                                    val targetSize = 256
                                    val sampleSize = (info.size.width / targetSize).coerceAtLeast(1)
                                    decoder.setTargetSampleSize(sampleSize)
                                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(context.contentResolver, albumArtUri)
                            }
                        }
                        else -> null
                    }
                }
                bitmap?.let { dominantColorState.updateFrom(it.asImageBitmap()) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    val seedColor = if (settings.useAlbumArtColor && dominantColorState.color != Color.Unspecified) {
        dominantColorState.color
    } else {
        Color(settings.seedColor)
    }

    // 3. Typography
    val currentFontFamily = when(settings.appFontFamily) {
        AppFontFamily.GOOGLE_SANS -> GoogleSans
        AppFontFamily.JETBRAINS_MONO -> JetBrainsMonoNerd
        AppFontFamily.NUNITO -> Nunito
        AppFontFamily.DEFAULT -> androidx.compose.ui.text.font.FontFamily.Default
    }
    
    val currentTypography = getTypography(currentFontFamily)

    JasmineTheme(
        darkTheme = darkTheme,
        amoledMode = settings.amoledDarkMode,
        useDynamicColor = settings.useDynamicColor,
        seedColor = seedColor,
        paletteStyle = settings.paletteStyle,
        typography = currentTypography,
        content = content
    )
}
