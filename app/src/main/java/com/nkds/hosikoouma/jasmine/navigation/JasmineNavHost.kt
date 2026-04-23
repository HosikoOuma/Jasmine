package com.nkds.hosikoouma.jasmine.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nkds.hosikoouma.jasmine.datamodels.Screen
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.data.RadioStation
import com.nkds.hosikoouma.jasmine.ui.screens.*
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.RadioViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.MaintenanceViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun JasmineNavHost(
    navController: NavHostController,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    radioViewModel: RadioViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToRadioPlayer: () -> Unit,
    selectedTracks: Set<Track>,
    onToggleTrackSelection: (Track) -> Unit,
    selectedStations: Set<RadioStation>,
    onToggleStationSelection: (RadioStation) -> Unit,
    onAddTracksToPlaylist: () -> Unit,
    showAddRadioDialog: Boolean,
    onDismissRadioDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Tracks.route,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
        }
    ) {
        composable(Screen.Tracks.route) { 
            TracksScreen(
                trackViewModel = trackViewModel, 
                playerViewModel = playerViewModel,
                onNavigateToPlayer = onNavigateToPlayer,
                selectedTracks = selectedTracks,
                onToggleTrackSelection = onToggleTrackSelection
            ) 
        }
        composable(Screen.Radio.route) {
            RadioScreen(
                viewModel = radioViewModel,
                playerViewModel = playerViewModel,
                showAddDialog = showAddRadioDialog,
                onDismissDialog = onDismissRadioDialog,
                onStationClick = { station ->
                    val stations = radioViewModel.stations.value
                    playerViewModel.playRadio(station, stations)
                    onNavigateToRadioPlayer()
                },
                selectedStations = selectedStations,
                onToggleSelection = onToggleStationSelection
            ) 
        }
        composable(Screen.Library.route) { 
            LibraryScreen(navController = navController, trackViewModel = trackViewModel) 
        }
        
        // --- Настройки ---
        composable(Screen.Settings.route) { 
            SettingsScreen(navController = navController) 
        }
        composable(Screen.SettingsPlayback.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            PlaybackSettingsScreen(viewModel = settingsViewModel)
        }
        composable(Screen.SettingsAppearance.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            AppearanceSettingsScreen(viewModel = settingsViewModel)
        }
        composable(Screen.SettingsLibrary.route) {
            LibrarySettingsScreen(trackViewModel = trackViewModel)
        }
        composable(Screen.SettingsMaintenance.route) {
            MaintenanceScreen()
        }
        composable(Screen.Statistics.route) {
            StatisticsScreen()
        }
        composable(Screen.About.route) {
            AboutScreen()
        }

        // --- Библиотека ---
        composable(Screen.LibraryAlbums.route) { 
            AlbumListScreen(navController = navController, trackViewModel = trackViewModel) 
        }
        composable(Screen.LibraryArtists.route) { 
            ArtistListScreen(navController = navController, trackViewModel = trackViewModel) 
        }
        composable(Screen.LibraryFolders.route) { 
            FolderListScreen(navController = navController, trackViewModel = trackViewModel) 
        }
        composable(Screen.LibraryPlaylists.route) { 
            PlaylistListScreen(navController = navController, trackViewModel = trackViewModel) 
        }

        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(navArgument("albumName") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("albumName") ?: ""
            val albumName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
            AlbumDetailScreen(
                albumName = albumName,
                navController = navController,
                trackViewModel = trackViewModel,
                playerViewModel = playerViewModel,
                onNavigateToPlayer = onNavigateToPlayer,
                selectedTracks = selectedTracks,
                onToggleTrackSelection = onToggleTrackSelection
            )
        }

        composable(
            route = Screen.ArtistDetail.route,
            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("artistName") ?: ""
            val artistName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
            ArtistDetailScreen(
                artistName = artistName,
                navController = navController,
                trackViewModel = trackViewModel,
                playerViewModel = playerViewModel,
                onNavigateToPlayer = onNavigateToPlayer,
                selectedTracks = selectedTracks,
                onToggleTrackSelection = onToggleTrackSelection
            )
        }

        composable(
            route = Screen.FolderDetail.route,
            arguments = listOf(navArgument("folderPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("folderPath") ?: ""
            val folderPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
            FolderDetailScreen(
                folderPath = folderPath,
                navController = navController,
                trackViewModel = trackViewModel,
                playerViewModel = playerViewModel,
                onNavigateToPlayer = onNavigateToPlayer,
                selectedTracks = selectedTracks,
                onToggleTrackSelection = onToggleTrackSelection
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            PlaylistDetailScreen(
                playlistId = playlistId,
                navController = navController,
                trackViewModel = trackViewModel,
                playerViewModel = playerViewModel,
                onNavigateToPlayer = onNavigateToPlayer,
                onAddTracksClick = onAddTracksToPlaylist,
                selectedTracks = selectedTracks,
                onToggleTrackSelection = onToggleTrackSelection
            )
        }
    }
}
