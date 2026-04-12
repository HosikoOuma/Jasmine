package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.nkds.hosikoouma.jasmine.R

@Composable
fun AlbumArt(
    albumArtUri: Any?, // Изменено на Any? для поддержки ByteArray
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    contentScale: ContentScale = ContentScale.Crop
) {
    var isSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(albumArtUri)
                .error(R.drawable.ison_vec)
                .fallback(R.drawable.ison_vec)
                .crossfade(true)
                .build(),
            contentDescription = null,
            onState = { state ->
                isSuccess = state is AsyncImagePainter.State.Success
            },
            contentScale = if (isSuccess) contentScale else ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSuccess) 0.dp else 8.dp)
        )
    }
}
