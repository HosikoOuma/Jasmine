package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.datamodels.Folder
import com.nkds.hosikoouma.jasmine.ui.components.LibraryItemCard
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- UI State ---
data class FolderListUiState(
    val folders: List<Folder> = emptyList(),
    val isLoading: Boolean = false
)

// --- Stateful Screen ---
@Composable
fun FolderListScreen(
    navController: NavController,
    trackViewModel: TrackViewModel
) {
    val folders by trackViewModel.folders.collectAsStateWithLifecycle()

    FolderListContent(
        uiState = FolderListUiState(folders = folders),
        onFolderClick = { folder ->
            val encodedPath = URLEncoder.encode(folder.path, StandardCharsets.UTF_8.toString())
            navController.navigate("folder_detail/$encodedPath")
        }
    )
}

// --- Stateless Content ---
@Composable
fun FolderListContent(
    uiState: FolderListUiState,
    onFolderClick: (Folder) -> Unit
) {
    if (uiState.folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No music folders found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 160.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.folders, key = { it.path }) { folder ->
                LibraryItemCard(
                    title = folder.name,
                    subtitle = "${folder.tracks.size} tracks",
                    icon = Icons.Rounded.Folder,
                    onClick = { onFolderClick(folder) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FolderListPreview() {
    JasmineTheme {
        FolderListContent(
            uiState = FolderListUiState(
                folders = listOf(
                    Folder("Music", "/storage/emulated/0/Music", emptyList()),
                    Folder("Downloads", "/storage/emulated/0/Download", emptyList())
                )
            ),
            onFolderClick = {}
        )
    }
}
