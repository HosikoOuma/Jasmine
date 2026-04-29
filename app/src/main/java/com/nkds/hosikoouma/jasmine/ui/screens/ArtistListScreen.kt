package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.R
import com.nkds.hosikoouma.jasmine.datamodels.Artist
import com.nkds.hosikoouma.jasmine.ui.components.LibraryItemCard
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- UI State ---
data class ArtistListUiState(
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = false
)

// --- Stateful Screen ---
@Composable
fun ArtistListScreen(
    navController: NavController,
    trackViewModel: TrackViewModel
) {
    val artists by trackViewModel.artists.collectAsStateWithLifecycle()

    ArtistListContent(
        uiState = ArtistListUiState(artists = artists),
        onArtistClick = { artist ->
            val encodedName = URLEncoder.encode(artist.name, StandardCharsets.UTF_8.toString())
            navController.navigate("artist_detail/$encodedName")
        }
    )
}

// --- Stateless Content ---
@Composable
fun ArtistListContent(
    uiState: ArtistListUiState,
    onArtistClick: (Artist) -> Unit
) {
    if (uiState.artists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(stringResource(R.string.no_artists), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 160.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.artists, key = { it.name }) { artist ->
                LibraryItemCard(
                    title = artist.name,
                    subtitle = stringResource(R.string.tracks_count, artist.tracks.size),
                    icon = Icons.Rounded.Person,
                    onClick = { onArtistClick(artist) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ArtistListPreview() {
    JasmineTheme {
        ArtistListContent(
            uiState = ArtistListUiState(
                artists = listOf(
                    Artist("Jasmine", emptyList()),
                    Artist("Various Artists", emptyList())
                )
            ),
            onArtistClick = {}
        )
    }
}
