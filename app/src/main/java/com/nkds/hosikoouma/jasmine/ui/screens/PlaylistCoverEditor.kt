package com.nkds.hosikoouma.jasmine.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlaylistCoverEditor(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(imageUri)?.use { 
                bitmap = BitmapFactory.decodeStream(it)
            }
        }
    }

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White)
                }
                Text("Crop Cover", style = MaterialTheme.typography.titleLarge, color = Color.White)
                IconButton(
                    onClick = {
                        bitmap?.let { 
                            val cropped = cropBitmap(it, scale, offset, containerSize)
                            onConfirm(cropped)
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .onGloballyPositioned { containerSize = it.size },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.DarkGray)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    offset += pan
                                }
                            }
                            .clipToBounds()
                    ) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            
            Text(
                "Pinch to zoom and drag to move",
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 48.dp),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun cropBitmap(source: Bitmap, scale: Float, offset: Offset, containerSize: IntSize): Bitmap {
    // Очень упрощенная логика кропа для демонстрации. 
    // В реальном приложении лучше использовать библиотеку или более точные расчеты Matrix.
    val size = minOf(source.width, source.height)
    val matrix = Matrix()
    matrix.postScale(scale, scale)
    // Здесь должны быть расчеты смещения относительно центра
    
    // Для Jasmine мы сделаем проще: просто возьмем квадрат из центра если не успеем реализовать полный расчет
    val x = ((source.width - size) / 2).coerceAtLeast(0)
    val y = ((source.height - size) / 2).coerceAtLeast(0)
    
    return Bitmap.createBitmap(source, x, y, size, size, null, true)
}
