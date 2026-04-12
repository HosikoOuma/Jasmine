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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.kmpalette.rememberDominantColorState
import com.nkds.hosikoouma.jasmine.ui.MainScreen
import com.nkds.hosikoouma.jasmine.ui.screens.SplashScreen
import com.nkds.hosikoouma.jasmine.ui.theme.*
import com.nkds.hosikoouma.jasmine.viewmodels.DarkMode
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Глобальная настройка Coil для кэширования обложек
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val playerViewModel: PlayerViewModel = viewModel()
            
            val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()

            // Определяем, должна ли быть темная тема
            val darkTheme = when (settings.darkMode) {
                DarkMode.DARK.name -> true
                DarkMode.LIGHT.name -> false
                else -> isSystemInDarkTheme()
            }

            // Извлечение цвета из обложки
            val currentTrack by playerViewModel.currentTrack.collectAsStateWithLifecycle()
            val dominantColorState = rememberDominantColorState()
            val context = LocalContext.current

            LaunchedEffect(currentTrack?.albumArtUri, settings.useAlbumArtColor) {
                if (settings.useAlbumArtColor && currentTrack?.albumArtUri != null) {
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, currentTrack!!.albumArtUri!!))
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(context.contentResolver, currentTrack!!.albumArtUri)
                            }
                        }
                        dominantColorState.updateFrom(bitmap.asImageBitmap())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val seedColor = if (settings.useAlbumArtColor && currentTrack?.albumArtUri != null && dominantColorState.color != Color.Unspecified) {
                dominantColorState.color
            } else {
                Color(settings.seedColor)
            }

            // Выбираем FontFamily на основе настроек
            val currentFontFamily = when(settings.appFontFamily) {
                "GOOGLE_SANS" -> GoogleSans
                "JETBRAINS_MONO" -> JetBrainsMonoNerd
                "NUNITO" -> Nunito
                else -> androidx.compose.ui.text.font.FontFamily.Default
            }
            
            val currentTypography = getTypography(currentFontFamily)

            JasmineTheme(
                darkTheme = darkTheme,
                amoledMode = settings.amoledDarkMode,
                useDynamicColor = settings.useDynamicColor,
                seedColor = seedColor,
                paletteStyle = settings.paletteStyle,
                typography = currentTypography
            ) {
                val trackViewModel: TrackViewModel = viewModel()
                val isLoaded by trackViewModel.isLoaded.collectAsStateWithLifecycle()
                
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
