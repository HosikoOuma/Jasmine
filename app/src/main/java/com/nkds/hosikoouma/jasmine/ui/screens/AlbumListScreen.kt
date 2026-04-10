package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AlbumListScreen(
    navController: NavController,
    trackViewModel: TrackViewModel
) {
    val albums by trackViewModel.albums.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 160.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums) { album ->
            AlbumCard(
                albumName = album.name,
                artistName = album.artist,
                albumArtUri = album.tracks.firstOrNull()?.albumArtUri,
                onClick = {
                    val encodedName = URLEncoder.encode(album.name, StandardCharsets.UTF_8.toString())
                    navController.navigate("album_detail/$encodedName")
                }
            )
        }
    }
}

@Composable
fun AlbumCard(
    albumName: String,
    artistName: String,
    albumArtUri: android.net.Uri?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AlbumArt(
            albumArtUri = albumArtUri,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = albumName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = artistName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
