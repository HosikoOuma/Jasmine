package com.nkds.hosikoouma.jasmine.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.datamodels.Playlist
import com.nkds.hosikoouma.jasmine.ui.components.LibraryItemCard
import com.nkds.hosikoouma.jasmine.ui.components.bouncingClickable
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

// --- UI State ---
data class PlaylistListUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false
)

// --- Stateful Screen ---
@Composable
fun PlaylistListScreen(
    navController: NavController,
    trackViewModel: TrackViewModel
) {
    val playlistsData by trackViewModel.playlists.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            val name = cursor?.use { c ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst()) c.getString(nameIndex) else null
            }?.substringBeforeLast(".") ?: "Imported Playlist"
            
            trackViewModel.importPlaylistFromUri(it, name)
        }
    }

    PlaylistListContent(
        uiState = PlaylistListUiState(playlists = playlistsData),
        onPlaylistClick = { id -> navController.navigate("playlist_detail/$id") },
        onImportClick = { filePickerLauncher.launch("*/*") },
        onCreateClick = { showCreateDialog = true },
        trackViewModel = trackViewModel
    )

    if (showCreateDialog) {
        NewPlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                trackViewModel.createPlaylist(name)
                showCreateDialog = false
            }
        )
    }
}

// --- Stateless Content ---
@Composable
fun PlaylistListContent(
    uiState: PlaylistListUiState,
    onPlaylistClick: (Long) -> Unit,
    onImportClick: () -> Unit,
    onCreateClick: () -> Unit,
    trackViewModel: TrackViewModel? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                PlaylistActionButtons(onImportClick, onCreateClick)
            }

            if (uiState.playlists.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                        Text("No playlists found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.playlists, key = { it.id }) { playlist ->
                    // Получаем количество песен для каждого плейлиста
                    val tracksCount by if (trackViewModel != null) {
                        trackViewModel.getTracksForPlaylist(playlist.id).collectAsStateWithLifecycle(initialValue = emptyList())
                    } else {
                        remember { mutableStateOf(emptyList()) }
                    }

                    LibraryItemCard(
                        title = playlist.name,
                        subtitle = "${tracksCount.size} tracks",
                        icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        onClick = { onPlaylistClick(playlist.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistActionButtons(onImportClick: () -> Unit, onCreateClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.weight(1f).height(56.dp).bouncingClickable(onClick = onImportClick),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PostAdd, null)
                Spacer(Modifier.width(8.dp)); Text("From Device", style = MaterialTheme.typography.labelLarge)
            }
        }
        Surface(
            modifier = Modifier.weight(1f).height(56.dp).bouncingClickable(onClick = onCreateClick),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CreateNewFolder, null)
                Spacer(Modifier.width(8.dp)); Text("Create New", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun NewPlaylistDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var playlistName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = { TextField(value = playlistName, onValueChange = { playlistName = it }, placeholder = { Text("Playlist name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { if (playlistName.isNotBlank()) onConfirm(playlistName) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(28.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun PlaylistListPreview() {
    JasmineTheme {
        PlaylistListContent(
            uiState = PlaylistListUiState(
                playlists = listOf(Playlist(id = 1, name = "Favorites", emptyList()), Playlist(id = 2, name = "Gym", emptyList()))
            ),
            onPlaylistClick = {}, onImportClick = {}, onCreateClick = {}
        )
    }
}
