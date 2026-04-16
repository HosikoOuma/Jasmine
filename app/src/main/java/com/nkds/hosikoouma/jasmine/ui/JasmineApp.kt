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

@Composable
fun JasmineApp(
    settingsViewModel: SettingsViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    trackViewModel: TrackViewModel = viewModel()
) {
    val currentTrack by playerViewModel.currentTrack.collectAsStateWithLifecycle()
    val isLoaded by trackViewModel.isLoaded.collectAsStateWithLifecycle()
    val appToast by playerViewModel.appToast.collectAsStateWithLifecycle()

    JasmineThemeWrapper(
        albumArtUri = currentTrack?.albumArtUri,
        settingsViewModel = settingsViewModel
    ) {
        var animationFinished by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            if (!animationFinished || !isLoaded) {
                SplashScreen(onFinished = { animationFinished = true })
            } else {
                MainScreen(
                    trackViewModel = trackViewModel,
                    playerViewModel = playerViewModel
                )
            }

            // Кастомное уведомление поверх всего интерфейса
            AppToastContainer(
                toastData = appToast,
                onDismiss = { playerViewModel.clearAddedToast() }
            )
        }
    }
}
