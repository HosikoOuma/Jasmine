package com.nkds.hosikoouma.jasmine.ui.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerBackgroundStyle
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PlayerBackground(
    style: PlayerBackgroundStyle,
    albumArtUri: Uri?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when (style) {
            PlayerBackgroundStyle.STANDARD -> {
                // Просто стандартный фон поверхности (уже задан в Box)
            }
            PlayerBackgroundStyle.BLURRED_COVER -> {
                AsyncImage(
                    model = albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                        .background(Color.Black.copy(alpha = 0.3f))
                )
                // Затемнение поверх блюра для читаемости
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            }
            PlayerBackgroundStyle.AURA -> {
                AuraBackground()
            }
        }
    }
}

@Composable
fun AuraBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "aura")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val color1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val color2 = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    val color3 = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)

    Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
        val width = size.width
        val height = size.height
        val radius = maxOf(width, height)

        val x1 = width / 2 + radius * cos(Math.toRadians(angle.toDouble())).toFloat() * 0.5f
        val y1 = height / 2 + radius * sin(Math.toRadians(angle.toDouble())).toFloat() * 0.5f

        val x2 = width / 2 + radius * cos(Math.toRadians((angle + 120).toDouble())).toFloat() * 0.5f
        val y2 = height / 2 + radius * sin(Math.toRadians((angle + 120).toDouble())).toFloat() * 0.5f

        val x3 = width / 2 + radius * cos(Math.toRadians((angle + 240).toDouble())).toFloat() * 0.5f
        val y3 = height / 2 + radius * sin(Math.toRadians((angle + 240).toDouble())).toFloat() * 0.5f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color1, Color.Transparent),
                center = Offset(x1, y1),
                radius = radius
            ),
            center = Offset(x1, y1),
            radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color2, Color.Transparent),
                center = Offset(x2, y2),
                radius = radius
            ),
            center = Offset(x2, y2),
            radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color3, Color.Transparent),
                center = Offset(x3, y3),
                radius = radius
            ),
            center = Offset(x3, y3),
            radius = radius
        )
    }
}
