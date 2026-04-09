package com.nkds.hosikoouma.jasmine.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nkds.hosikoouma.jasmine.datamodels.Screen
import com.nkds.hosikoouma.jasmine.ui.screens.PlaceholderScreen
import com.nkds.hosikoouma.jasmine.ui.screens.SettingsScreen
import com.nkds.hosikoouma.jasmine.ui.screens.TracksScreen
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

@Composable
fun JasmineNavHost(
    navController: NavHostController,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Tracks.route,
        modifier = modifier
    ) {
        composable(Screen.Tracks.route) { 
            TracksScreen(
                trackViewModel = trackViewModel, 
                playerViewModel = playerViewModel,
                onNavigateToPlayer = onNavigateToPlayer
            ) 
        }
        composable(Screen.Radio.route) { PlaceholderScreen(Screen.Radio.icon) }
        composable(Screen.Library.route) { PlaceholderScreen(Screen.Library.icon) }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
