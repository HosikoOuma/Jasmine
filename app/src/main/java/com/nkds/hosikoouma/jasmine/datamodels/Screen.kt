package com.nkds.hosikoouma.jasmine.datamodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Tracks : Screen("tracks", "Tracks", Icons.Default.MusicNote)
    data object Radio : Screen("radio", "Radio", Icons.Default.Radio)
    data object Library : Screen("library", "Library", Icons.Default.LibraryMusic)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Player : Screen("player", "Player", Icons.Default.MusicNote)

    companion object {
        val items get() = listOf(Tracks, Radio, Library, Settings)
    }
}
