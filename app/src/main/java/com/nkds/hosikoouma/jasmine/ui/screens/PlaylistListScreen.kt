package com.nkds.hosikoouma.jasmine.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.datamodels.Playlist
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.LibraryItemCard
import com.nkds.hosikoouma.jasmine.ui.components.bouncingClickable
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

// --- UI State ---
data class PlaylistListUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val isGridView: Boolean = false
)

// --- Stateful Screen ---
@Composable
fun PlaylistListScreen(
    navController: NavController,
    trackViewModel: TrackViewModel
) {
    val playlistsData by trackViewModel.playlists.collectAsStateWithLifecycle()
    var isGridView by rememberSaveable { mutableStateOf(false) }
    
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
        uiState = PlaylistListUiState(playlists = playlistsData, isGridView = isGridView),
        onPlaylistClick = { id -> navController.navigate("playlist_detail/$id") },
        onImportClick = { filePickerLauncher.launch("*/*") },
        onCreateClick = { showCreateDialog = true },
        onToggleView = { isGridView = !isGridView },
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
    onToggleView: () -> Unit,
    trackViewModel: TrackViewModel? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistActionButtons(
                onImportClick = onImportClick,
                onCreateClick = onCreateClick,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onToggleView,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(if (uiState.isGridView) Icons.Rounded.List else Icons.Rounded.GridView, null)
            }
        }

        if (uiState.playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No playlists found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            if (uiState.isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.playlists, key = { it.id }) { playlist ->
                        val playlistTracks by if (trackViewModel != null) {
                            trackViewModel.getTracksForPlaylist(playlist.id).collectAsStateWithLifecycle(initialValue = emptyList())
                        } else {
                            remember { mutableStateOf(emptyList()) }
                        }
                        
                        PlaylistCard(
                            playlist = playlist,
                            tracksCount = playlistTracks.size,
                            // Приоритет: кастомная обложка > обложка первого трека > заглушка
                            displayArt = playlist.coverUri ?: playlistTracks.firstOrNull()?.albumArtUri,
                            onClick = { onPlaylistClick(playlist.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.playlists, key = { it.id }) { playlist ->
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
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    tracksCount: Int,
    displayArt: android.net.Uri?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        AlbumArt(
            albumArtUri = displayArt,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = "$tracksCount tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlaylistActionButtons(
    onImportClick: () -> Unit, 
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.weight(1f).height(48.dp).bouncingClickable(onClick = onImportClick),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PostAdd, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp)); Text("Import", style = MaterialTheme.typography.labelLarge)
            }
        }
        Surface(
            modifier = Modifier.weight(1f).height(48.dp).bouncingClickable(onClick = onCreateClick),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CreateNewFolder, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp)); Text("New", style = MaterialTheme.typography.labelLarge)
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
            onPlaylistClick = {}, onImportClick = {}, onCreateClick = {}, onToggleView = {}
        )
    }
}
