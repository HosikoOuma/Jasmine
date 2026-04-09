package com.nkds.hosikoouma.jasmine.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PlayerBackground(
    albumArtUri: Uri?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    Box(modifier = modifier.fillMaxSize().background(colorScheme.surface)) {
        var extractedColors by remember { mutableStateOf<List<Color>>(emptyList()) }

        LaunchedEffect(albumArtUri) {
            if (albumArtUri != null) {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .allowHardware(false)
                    .build()
                
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        withContext(Dispatchers.Default) {
                            val palette = Palette.from(bitmap).generate()
                            val colors = listOfNotNull(
                                palette.getVibrantColor(0).takeIf { it != 0 },
                                palette.getDominantColor(0).takeIf { it != 0 },
                                palette.getMutedColor(0).takeIf { it != 0 },
                                palette.getLightVibrantColor(0).takeIf { it != 0 },
                                palette.getDarkVibrantColor(0).takeIf { it != 0 },
                                palette.getLightMutedColor(0).takeIf { it != 0 }
                            ).map { Color(it) }.distinct()
                            
                            extractedColors = if (colors.size >= 3) colors.take(6)
                            else (colors + listOf(colorScheme.primary, colorScheme.secondary, colorScheme.tertiary)).take(3)
                        }
                    }
                }
            } else {
                extractedColors = listOf(colorScheme.primary, colorScheme.secondary, colorScheme.tertiary)
            }
        }

        AuraNebula(colors = extractedColors)
    }
}

@Composable
fun AuraNebula(colors: List<Color>) {
    if (colors.isEmpty()) return

    val infiniteTransition = rememberInfiniteTransition(label = "nebula")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize().blur(120.dp).alpha(0.8f)) {
            val w = size.width
            val h = size.height
            val baseRadius = maxOf(w, h) * 0.9f

            colors.forEachIndexed { index, targetColor ->
                val t = time * 2f * Math.PI.toFloat()
                val phase = (index.toFloat() / colors.size) * 2f * Math.PI.toFloat()

                // Lissajous curves for organic movement
                val x = w / 2 + (w * 0.35f * cos(t + phase * 1.5f)).toFloat()
                val y = h / 2 + (h * 0.35f * sin(t * 0.7f + phase)).toFloat()
                
                // Radius pulsation
                val radius = baseRadius * (1f + 0.25f * sin(t * 1.3f + phase))

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(targetColor.copy(alpha = 0.7f), Color.Transparent),
                        center = Offset(x, y),
                        radius = radius
                    ),
                    center = Offset(x, y),
                    radius = radius,
                    blendMode = BlendMode.Plus
                )
            }
        }
    }
}
