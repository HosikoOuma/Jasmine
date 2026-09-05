package com.nkds.hosikoouma.jasmine.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.nkds.hosikoouma.jasmine.ui.components.AppToastContainer
import com.nkds.hosikoouma.jasmine.ui.components.JasmineThemeWrapper
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.RadioViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TelegramCloudViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun JasmineApp() {
    val context = LocalContext.current
    
    // Пытаемся найти Activity в контексте (включая обертки для локали)
    val activity = remember(context) { context.findActivity() }
    
    // В качестве владельца ViewModel используем Activity или текущий LocalViewModelStoreOwner
    val viewModelStoreOwner = activity ?: LocalViewModelStoreOwner.current ?: (context as ViewModelStoreOwner)
    
    val settingsViewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner)
    val playerViewModel: PlayerViewModel = hiltViewModel(viewModelStoreOwner)
    val trackViewModel: TrackViewModel = hiltViewModel(viewModelStoreOwner)
    val radioViewModel: RadioViewModel = hiltViewModel(viewModelStoreOwner)
    val telegramCloudViewModel: TelegramCloudViewModel = hiltViewModel(viewModelStoreOwner)

    // 1. Оптимизация темы: подписываемся только на URI обложки, а не на весь объект Track.
    val albumArtUri by remember(playerViewModel) {
        playerViewModel.currentTrack
            .map { it?.albumArtUri }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)

    JasmineThemeWrapper(
        albumArtUri = albumArtUri,
        settingsViewModel = settingsViewModel
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MainScreen(
                trackViewModel = trackViewModel,
                playerViewModel = playerViewModel,
                radioViewModel = radioViewModel,
                telegramCloudViewModel = telegramCloudViewModel
            )

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

// Хелпер для поиска Activity в цепочке ContextWrapper
private fun Context.findActivity(): ComponentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}
