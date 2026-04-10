package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.datamodels.Screen
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

@Composable
fun LibraryScreen(
    navController: NavController,
    trackViewModel: TrackViewModel
) {
    val albums by trackViewModel.albums.collectAsState()
    val artists by trackViewModel.artists.collectAsState()
    val folders by trackViewModel.folders.collectAsState()

    val libraryItems = listOf(
        LibraryCategory("Playlists", Icons.AutoMirrored.Rounded.PlaylistPlay, 0, Screen.LibraryPlaylists.route),
        LibraryCategory("Albums", Icons.Rounded.Album, albums.size, Screen.LibraryAlbums.route),
        LibraryCategory("Artists", Icons.Rounded.Person, artists.size, Screen.LibraryArtists.route),
        LibraryCategory("Folders", Icons.Rounded.Folder, folders.size, Screen.LibraryFolders.route)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            items(libraryItems) { item ->
                LibraryCard(item) {
                    navController.navigate(item.route)
                }
            }
        }
    }
}

data class LibraryCategory(
    val title: String,
    val icon: ImageVector,
    val count: Int,
    val route: String
)

@Composable
fun LibraryCard(
    category: LibraryCategory,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Column {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${category.count} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
