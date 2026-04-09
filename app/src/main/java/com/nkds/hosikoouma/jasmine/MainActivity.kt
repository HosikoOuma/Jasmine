package com.nkds.hosikoouma.jasmine

import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmpalette.rememberDominantColorState
import com.nkds.hosikoouma.jasmine.ui.MainScreen
import com.nkds.hosikoouma.jasmine.ui.screens.SplashScreen
import com.nkds.hosikoouma.jasmine.ui.theme.*
import com.nkds.hosikoouma.jasmine.viewmodels.DarkMode
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val playerViewModel: PlayerViewModel = viewModel()
            
            val fontStyle by settingsViewModel.appFontFamily.collectAsState()
            val darkModeSetting by settingsViewModel.darkMode.collectAsState()
            val amoledMode by settingsViewModel.amoledDarkMode.collectAsState()
            val useDynamicColor by settingsViewModel.useDynamicColor.collectAsState()
            val paletteStyle by settingsViewModel.paletteStyle.collectAsState()
            val useAlbumArtColor by settingsViewModel.useAlbumArtColor.collectAsState()
            val savedSeedColorInt by settingsViewModel.seedColor.collectAsState()

            // Определяем, должна ли быть темная тема
            val darkTheme = when (darkModeSetting) {
                DarkMode.DARK.name -> true
                DarkMode.LIGHT.name -> false
                else -> isSystemInDarkTheme()
            }

            // Извлечение цвета из обложки
            val currentTrack by playerViewModel.currentTrack.collectAsState()
            val dominantColorState = rememberDominantColorState()
            val context = LocalContext.current

            LaunchedEffect(currentTrack?.albumArtUri, useAlbumArtColor) {
                if (useAlbumArtColor && currentTrack?.albumArtUri != null) {
                    try {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, currentTrack!!.albumArtUri!!))
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(context.contentResolver, currentTrack!!.albumArtUri)
                        }
                        dominantColorState.updateFrom(bitmap.asImageBitmap())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val seedColor = if (useAlbumArtColor && currentTrack?.albumArtUri != null && dominantColorState.color != Color.Unspecified) {
                dominantColorState.color
            } else {
                Color(savedSeedColorInt)
            }

            // Выбираем FontFamily на основе настроек
            val currentFontFamily = when(fontStyle) {
                "GOOGLE_SANS" -> GoogleSans
                "JETBRAINS_MONO" -> JetBrainsMonoNerd
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
                val trackViewModel: TrackViewModel = viewModel()
                val isLoaded by trackViewModel.isLoaded.collectAsState()
                
                var animationFinished by remember { mutableStateOf(false) }

                if (!animationFinished || !isLoaded) {
                    SplashScreen(onFinished = { animationFinished = true })
                } else {
                    MainScreen(trackViewModel = trackViewModel)
                }
            }
        }
    }
}
