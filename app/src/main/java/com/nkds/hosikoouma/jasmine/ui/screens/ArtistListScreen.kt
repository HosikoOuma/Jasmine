package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.ui.components.LibraryItemCard
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun ArtistListScreen(
    navController: NavController,
    trackViewModel: TrackViewModel
) {
    val artists by trackViewModel.artists.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 160.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(artists) { artist ->
            LibraryItemCard(
                title = artist.name,
                subtitle = "${artist.tracks.size} tracks",
                icon = Icons.Rounded.Person,
                // Для артиста можно брать обложку первого трека как аватар, если нужно
                // imageUri = artist.tracks.firstOrNull()?.albumArtUri,
                onClick = {
                    val encodedName = URLEncoder.encode(artist.name, StandardCharsets.UTF_8.toString())
                    navController.navigate("artist_detail/$encodedName")
                }
            )
        }
    }
}
