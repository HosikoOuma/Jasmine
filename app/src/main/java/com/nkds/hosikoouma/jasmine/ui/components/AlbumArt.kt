package com.nkds.hosikoouma.jasmine.ui.components

import android.graphics.Bitmap
import android.os.Build
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.nkds.hosikoouma.jasmine.R

@Composable
fun AlbumArt(
    albumArtUri: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    contentScale: ContentScale = ContentScale.Crop,
    isLowRes: Boolean = false,
    cacheKey: String? = null // Добавляем ключ для стабильного кэширования
) {
    var isSuccess by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Формируем ключ: если это URI, добавляем префикс. 
    // Это поможет Coil отличать обложки разных треков, даже если URI пуст или одинаков.
    val finalRegistryKey = remember(albumArtUri, cacheKey) {
        cacheKey ?: albumArtUri?.toString()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(albumArtUri)
                .memoryCacheKey(finalRegistryKey)
                .diskCacheKey(finalRegistryKey)
                .error(R.drawable.ison_vec)
                .fallback(R.drawable.ison_vec)
                .crossfade(true)
                .apply {
                    if (isLowRes) {
                        precision(Precision.INEXACT)
                        size(200, 200) // Немного увеличим для четкости на современных экранах
                        bitmapConfig(Bitmap.Config.RGB_565)
                    } else {
                        precision(Precision.INEXACT)
                        size(800, 800) 
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            bitmapConfig(Bitmap.Config.HARDWARE)
                        }
                    }
                }
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            onState = { state ->
                isSuccess = state is AsyncImagePainter.State.Success
            },
            contentScale = if (isSuccess) contentScale else ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSuccess) 0.dp else 12.dp)
        )
    }
}
