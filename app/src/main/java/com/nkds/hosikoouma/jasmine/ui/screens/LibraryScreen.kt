package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nkds.hosikoouma.jasmine.datamodels.Screen
import com.nkds.hosikoouma.jasmine.ui.components.bouncingClickable
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

// --- UI State ---
data class LibraryUiState(
    val playlistCount: Int = 0,
    val albumCount: Int = 0,
    val artistCount: Int = 0,
    val folderCount: Int = 0
)

// --- Stateful Screen ---
@Composable
fun LibraryScreen(
    navController: NavController,
    trackViewModel: TrackViewModel
) {
    val albums by trackViewModel.albums.collectAsStateWithLifecycle()
    val artists by trackViewModel.artists.collectAsStateWithLifecycle()
    val folders by trackViewModel.folders.collectAsStateWithLifecycle()
    val playlists by trackViewModel.playlists.collectAsStateWithLifecycle()

    val uiState = LibraryUiState(
        playlistCount = playlists.size,
        albumCount = albums.size,
        artistCount = artists.size,
        folderCount = folders.size
    )

    LibraryContent(
        uiState = uiState,
        onCategoryClick = { route -> navController.navigate(route) }
    )
}

// --- Stateless Content ---
@Composable
fun LibraryContent(
    uiState: LibraryUiState,
    onCategoryClick: (String) -> Unit
) {
    val categories = remember(uiState) {
        listOf(
            LibraryCategory("Playlists", Icons.AutoMirrored.Rounded.PlaylistPlay, uiState.playlistCount, Screen.LibraryPlaylists.route),
            LibraryCategory("Albums", Icons.Rounded.Album, uiState.albumCount, Screen.LibraryAlbums.route),
            LibraryCategory("Artists", Icons.Rounded.Person, uiState.artistCount, Screen.LibraryArtists.route),
            LibraryCategory("Folders", Icons.Rounded.Folder, uiState.folderCount, Screen.LibraryFolders.route)
        )
    }

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
            items(categories) { category ->
                LibraryCard(category) {
                    onCategoryClick(category.route)
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
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .bouncingClickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
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

@Preview(showBackground = true)
@Composable
fun LibraryPreview() {
    JasmineTheme {
        LibraryContent(
            uiState = LibraryUiState(10, 5, 3, 4),
            onCategoryClick = {}
        )
    }
}
