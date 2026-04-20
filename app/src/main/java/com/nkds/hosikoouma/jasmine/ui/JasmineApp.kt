package com.nkds.hosikoouma.jasmine.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkds.hosikoouma.jasmine.ui.components.AppToastContainer
import com.nkds.hosikoouma.jasmine.ui.components.JasmineThemeWrapper
import com.nkds.hosikoouma.jasmine.ui.screens.SplashScreen
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun JasmineApp(
    settingsViewModel: SettingsViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    trackViewModel: TrackViewModel = viewModel()
) {
    // 1. Оптимизация темы: подписываемся только на URI обложки, а не на весь объект Track.
    // Это предотвратит рекомпозицию темы при смене метаданных (названия и т.д.), если обложка та же.
    val albumArtUri by remember(playerViewModel) {
        playerViewModel.currentTrack
            .map { it?.albumArtUri }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)

    val isLoaded by trackViewModel.isLoaded.collectAsStateWithLifecycle()

    JasmineThemeWrapper(
        albumArtUri = albumArtUri,
        settingsViewModel = settingsViewModel
    ) {
        var animationFinished by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            if (!animationFinished || !isLoaded) {
                SplashScreen(onFinished = { animationFinished = true })
            } else {
                // MainScreen теперь не рекомпозируется при каждом Toast или мелком изменении трека
                MainScreen(
                    trackViewModel = trackViewModel,
                    playerViewModel = playerViewModel
                )
            }

            // 2. Изоляция Toast: выносим подписку на стейт внутрь контейнера
            OptimizedAppToastContainer(playerViewModel)
        }
    }
}

@Composable
fun OptimizedAppToastContainer(playerViewModel: PlayerViewModel) {
    val appToast by playerViewModel.appToast.collectAsStateWithLifecycle()
    AppToastContainer(
        toastData = appToast,
        onDismiss = { playerViewModel.clearAddedToast() }
    )
}
