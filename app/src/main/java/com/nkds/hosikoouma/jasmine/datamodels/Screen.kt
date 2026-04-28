package com.nkds.hosikoouma.jasmine.datamodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Tracks : Screen("tracks", "Tracks", Icons.Rounded.MusicNote)
    data object Radio : Screen("radio", "Radio", Icons.Rounded.Radio)
    data object Library : Screen("library", "Library", Icons.Rounded.LibraryMusic)
    data object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
    data object Player : Screen("player", "Player", Icons.Rounded.MusicNote)
    
    // Вкладки панели навигации и экран управления каналами
    data object TelegramCloud : Screen("telegram_cloud_channels", "Cloud", Icons.Rounded.Cloud)
    data object LibraryPlaylists : Screen("library_playlists", "Playlists", Icons.AutoMirrored.Rounded.PlaylistPlay)
    
    // Экраны настроек
    data object SettingsPlayback : Screen("settings_playback", "Playback", Icons.Rounded.Settings)
    data object SettingsAppearance : Screen("settings_appearance", "Appearance", Icons.Rounded.Settings)
    data object SettingsShapes : Screen("settings_shapes", "Shapes Gallery", Icons.Rounded.Category)
    data object SettingsLibrary : Screen("settings_library", "Library", Icons.Rounded.Settings)
    data object SettingsMaintenance : Screen("settings_maintenance", "Maintenance", Icons.Rounded.Settings)
    data object SettingsTelegram : Screen("settings_telegram", "Telegram Cloud", Icons.Rounded.Cloud)
    data object About : Screen("about", "About", Icons.Rounded.Info)
    
    // Вложенные экраны библиотеки
    data object LibraryAlbums : Screen("library_albums", "Albums", Icons.Rounded.LibraryMusic)
    data object LibraryArtists : Screen("library_artists", "Artists", Icons.Rounded.LibraryMusic)
    data object LibraryFolders : Screen("library_folders", "Folders", Icons.Rounded.LibraryMusic)
    
    // Детальные экраны
    data object AlbumDetail : Screen("album_detail/{albumName}", "Album", Icons.Rounded.LibraryMusic)
    data object ArtistDetail : Screen("artist_detail/{artistName}", "Artist", Icons.Rounded.LibraryMusic)
    data object FolderDetail : Screen("folder_detail/{folderPath}", "Folder", Icons.Rounded.LibraryMusic)
    data object PlaylistDetail : Screen("playlist_detail/{playlistId}", "Playlist", Icons.Rounded.LibraryMusic)
    data object TelegramChannelDetail : Screen("telegram_channel_detail/{chatId}", "Channel", Icons.Rounded.Cloud)
    data object TelegramAuth : Screen("telegram_auth", "Telegram Login", Icons.AutoMirrored.Rounded.Login)
    data object Queue : Screen("queue", "Play Queue", Icons.AutoMirrored.Rounded.List)
    data object TelegramChatPicker : Screen("telegram_chat_picker", "Select Chat", Icons.Rounded.Chat)

    companion object {
        val allMainItems = listOf(Tracks, Radio, Library, TelegramCloud, LibraryPlaylists, Settings)
        
        fun getNavigationItems(routes: List<String>): List<Screen> {
            return routes.mapNotNull { route -> 
                allMainItems.find { it.route == route }
            }
        }

        val items get() = listOf(Tracks, Radio, Library, Settings)
    }
}
