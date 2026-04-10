package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    onDismissRequest: () -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    trackViewModel: TrackViewModel
) {
    val playlists by trackViewModel.playlists.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        },
        title = { Text("Add to playlist") },
        text = {
            if (playlists.isEmpty()) {
                Text("No playlists found. Create one in Library first.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    items(playlists) { playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name) },
                            leadingContent = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                            modifier = Modifier.clickable {
                                onPlaylistSelected(playlist.id)
                            }
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
