package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkds.hosikoouma.jasmine.datamodels.Playlist
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    onDismissRequest: () -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    trackViewModel: TrackViewModel
) {
    val playlists by trackViewModel.playlists.collectAsStateWithLifecycle()
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    if (selectedPlaylist == null) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismissRequest) { Text("Cancel") }
            },
            title = { Text("Add to playlist", fontWeight = FontWeight.Bold) },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists found. Create one in Library first.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(playlists) { playlist ->
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        playlist.name, 
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    ) 
                                },
                                leadingContent = { 
                                    AlbumArt(
                                        albumArtUri = playlist.coverUri,
                                        modifier = Modifier.size(48.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        isLowRes = true
                                    )
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedPlaylist = playlist },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(32.dp)
        )
    } else {
        AlertDialog(
            onDismissRequest = { selectedPlaylist = null },
            title = { Text("Confirmation") },
            text = { Text("Add to \"${selectedPlaylist?.name}\"?") },
            confirmButton = {
                Button(
                    onClick = { 
                        selectedPlaylist?.let { onPlaylistSelected(it.id) }
                        onDismissRequest() 
                    },
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { selectedPlaylist = null }) { Text("Back") }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
