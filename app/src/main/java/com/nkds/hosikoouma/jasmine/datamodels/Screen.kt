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

    companion object {
        val items get() = listOf(Tracks, Radio, Library, Settings)
    }
}
