package com.nkds.hosikoouma.jasmine.datamodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Tracks : Screen("tracks", "Tracks", Icons.Rounded.MusicNote)
    data object Radio : Screen("radio", "Radio", Icons.Rounded.Radio)
    data object Library : Screen("library", "Library", Icons.Rounded.LibraryMusic)
    data object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
    data object Player : Screen("player", "Player", Icons.Rounded.MusicNote)
    
    // Вложенные экраны библиотеки
    data object LibraryAlbums : Screen("library_albums", "Albums", Icons.Rounded.LibraryMusic)
    data object LibraryArtists : Screen("library_artists", "Artists", Icons.Rounded.LibraryMusic)
    data object LibraryFolders : Screen("library_folders", "Folders", Icons.Rounded.LibraryMusic)
    data object LibraryPlaylists : Screen("library_playlists", "Playlists", Icons.Rounded.LibraryMusic)
    
    // Детальные экраны
    data object AlbumDetail : Screen("album_detail/{albumName}", "Album", Icons.Rounded.LibraryMusic)
    data object ArtistDetail : Screen("artist_detail/{artistName}", "Artist", Icons.Rounded.LibraryMusic)
    data object FolderDetail : Screen("folder_detail/{folderPath}", "Folder", Icons.Rounded.LibraryMusic)
    data object PlaylistDetail : Screen("playlist_detail/{playlistId}", "Playlist", Icons.Rounded.LibraryMusic)

    companion object {
        val items get() = listOf(Tracks, Radio, Library, Settings)
    }
}
