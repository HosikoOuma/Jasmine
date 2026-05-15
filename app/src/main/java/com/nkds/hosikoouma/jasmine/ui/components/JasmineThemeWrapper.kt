package com.nkds.hosikoouma.jasmine.ui.components

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.kmpalette.rememberDominantColorState
import com.nkds.hosikoouma.jasmine.core.models.AppFontFamily
import com.nkds.hosikoouma.jasmine.core.models.DarkMode
import com.nkds.hosikoouma.jasmine.ui.theme.*
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel

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
            val isPlaceholder = albumArtUri?.toString()?.contains("drawable/ison_vec") == true
            val data = if (isPlaceholder) null else (albumArtBytes ?: albumArtUri)

            if (data != null) {
                val request = ImageRequest.Builder(context)
                    .data(data)
                    .size(256) // Достаточно для анализа цветов
                    .allowHardware(false) // Нужно для извлечения пикселей
                    .build()
                
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    bitmap?.let { dominantColorState.updateFrom(it.asImageBitmap()) }
                }
            } else {
                dominantColorState.reset()
            }
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
