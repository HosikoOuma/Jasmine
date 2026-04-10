package com.nkds.hosikoouma.jasmine.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nkds.hosikoouma.jasmine.datamodels.Screen
import com.nkds.hosikoouma.jasmine.ui.screens.*
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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
        composable(Screen.Library.route) { 
            LibraryScreen(navController = navController, trackViewModel = trackViewModel) 
        }
        composable(Screen.Settings.route) { SettingsScreen() }
        
        // Вложенные экраны библиотеки
        composable(Screen.LibraryAlbums.route) { 
            AlbumListScreen(navController = navController, trackViewModel = trackViewModel) 
        }
        composable(Screen.LibraryArtists.route) { PlaceholderScreen(Icons.Rounded.Person) }
        composable(Screen.LibraryFolders.route) { PlaceholderScreen(Icons.Rounded.Folder) }
        composable(Screen.LibraryPlaylists.route) { PlaceholderScreen(Icons.AutoMirrored.Rounded.PlaylistPlay) }

        // Детальные экраны
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
                onNavigateToPlayer = onNavigateToPlayer
            )
        }
    }
}
