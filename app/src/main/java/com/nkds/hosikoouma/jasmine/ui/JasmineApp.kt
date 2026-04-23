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
import com.nkds.hosikoouma.jasmine.viewmodels.RadioViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun JasmineApp(
    settingsViewModel: SettingsViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    trackViewModel: TrackViewModel = viewModel(),
    radioViewModel: RadioViewModel = viewModel()
) {
    // 1. Оптимизация темы: подписываемся только на URI обложки, а не на весь объект Track.
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
                MainScreen(
                    trackViewModel = trackViewModel,
                    playerViewModel = playerViewModel,
                    radioViewModel = radioViewModel
                )
            }

            // 2. Изоляция Toast: слушаем тосты из всех источников
            AppToastManager(playerViewModel, trackViewModel, radioViewModel)
        }
    }
}

@Composable
fun AppToastManager(
    playerViewModel: PlayerViewModel, 
    trackViewModel: TrackViewModel,
    radioViewModel: RadioViewModel
) {
    val playerToast by playerViewModel.appToast.collectAsStateWithLifecycle()
    val trackToast by trackViewModel.appToast.collectAsStateWithLifecycle()
    val radioToast by radioViewModel.appToast.collectAsStateWithLifecycle()
    
    AppToastContainer(
        toastData = playerToast,
        onDismiss = { playerViewModel.clearAddedToast() }
    )
    
    AppToastContainer(
        toastData = trackToast,
        onDismiss = { trackViewModel.clearToast() }
    )

    AppToastContainer(
        toastData = radioToast,
        onDismiss = { radioViewModel.clearToast() }
    )
}
