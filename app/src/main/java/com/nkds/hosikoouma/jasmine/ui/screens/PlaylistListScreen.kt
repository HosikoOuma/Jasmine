package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.ui.components.LibraryItemCard
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

@Composable
fun PlaylistListScreen(
    navController: NavController,
    trackViewModel: TrackViewModel
) {
    val playlists by trackViewModel.playlists.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No playlists yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlists) { playlist ->
                    LibraryItemCard(
                        title = playlist.name,
                        subtitle = "Playlist",
                        icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        onClick = {
                            navController.navigate("playlist_detail/${playlist.id}")
                        }
                    )
                }
            }
        }
    }
}
