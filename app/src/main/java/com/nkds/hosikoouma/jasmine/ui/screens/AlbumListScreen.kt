package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.datamodels.Album
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.gridVerticalScrollbar
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- UI State ---
data class AlbumListUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = false
)

// --- Stateful Screen ---
@Composable
fun AlbumListScreen(
    navController: NavController,
    trackViewModel: TrackViewModel
) {
    val albums by trackViewModel.albums.collectAsStateWithLifecycle()

    AlbumListContent(
        uiState = AlbumListUiState(albums = albums),
        onAlbumClick = { album ->
            val encodedName = URLEncoder.encode(album.name, StandardCharsets.UTF_8.toString())
            navController.navigate("album_detail/$encodedName")
        }
    )
}

// --- Stateless Content ---
@Composable
fun AlbumListContent(
    uiState: AlbumListUiState,
    onAlbumClick: (Album) -> Unit
) {
    val gridState = rememberLazyGridState()

    if (uiState.albums.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No albums found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 160.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .gridVerticalScrollbar(gridState)
        ) {
            items(uiState.albums, key = { it.name + it.artist }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album) }
                )
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AlbumArt(
            albumArtUri = album.tracks.firstOrNull()?.albumArtUri,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AlbumListPreview() {
    JasmineTheme {
        AlbumListContent(
            uiState = AlbumListUiState(
                albums = listOf(
                    Album("Luminous", "Jasmine", emptyList()),
                    Album("Garden", "Various Artists", emptyList())
                )
            ),
            onAlbumClick = {}
        )
    }
}
