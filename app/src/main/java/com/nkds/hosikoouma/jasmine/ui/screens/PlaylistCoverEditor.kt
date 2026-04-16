package com.nkds.hosikoouma.jasmine.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

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
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    // Read bounds first
                    BitmapFactory.decodeStream(inputStream, null, options)
                    
                    // Re-open stream to decode actual bitmap with reasonable size
                    context.contentResolver.openInputStream(imageUri)?.use { actualStream ->
                        val sampleOptions = BitmapFactory.Options().apply {
                            // Don't load massive images, 2048 is plenty for a cover
                            inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, 2048, 2048)
                        }
                        bitmap = BitmapFactory.decodeStream(actualStream, null, sampleOptions)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        if (bitmap == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
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
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White)
                    }
                    Text("Crop Cover", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    IconButton(
                        onClick = {
                            val result = cropBitmap(bitmap!!, scale, offset, viewportSize)
                            onConfirm(result)
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
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Viewport (The square area)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .onGloballyPositioned { viewportSize = it.size }
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.DarkGray)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 5f)
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
                        
                        // Optional: Viewport border to show what will be cropped
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }
                }

                Text(
                    "Pinch to zoom and drag to move",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 48.dp),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, height2: Int): Int {
    var inSampleSize = 1
    if (height > height2 || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= height2 && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

private fun cropBitmap(source: Bitmap, scale: Float, offset: Offset, viewportSize: IntSize): Bitmap {
    if (viewportSize.width == 0 || viewportSize.height == 0) return source

    // Calculate the scale that ContentScale.Fit applied
    val bitmapWidth = source.width.toFloat()
    val bitmapHeight = source.height.toFloat()
    val viewWidth = viewportSize.width.toFloat()
    val viewHeight = viewportSize.height.toFloat()

    val baseScale = minOf(viewWidth / bitmapWidth, viewHeight / bitmapHeight)
    
    val finalScale = baseScale * scale
    
    // The center of the viewport in screen coordinates is viewportSize / 2
    // The image's top-left in viewport coordinate when scale=1, offset=0 is:
    val initialLeft = (viewWidth - bitmapWidth * baseScale) / 2
    val initialTop = (viewHeight - bitmapHeight * baseScale) / 2
    
    // Current center of the bitmap in viewport coordinates:
    val bitmapCenterXInView = initialLeft + (bitmapWidth * baseScale) / 2 + offset.x
    val bitmapCenterYInView = initialTop + (bitmapHeight * baseScale) / 2 + offset.y
    
    // We want a square of size viewWidth (since aspectRatio=1)
    // The viewport's left/top/right/bottom in its own coordinates:
    // left = 0, top = 0, right = viewWidth, bottom = viewHeight
    
    // Convert view coordinates back to bitmap coordinates
    // bitmapX = (viewX - bitmapCenterXInView) / finalScale + bitmapWidth/2
    
    val cropLeftInBitmap = (0f - bitmapCenterXInView) / finalScale + bitmapWidth / 2
    val cropTopInBitmap = (0f - bitmapCenterYInView) / finalScale + bitmapHeight / 2
    val cropRightInBitmap = (viewWidth - bitmapCenterXInView) / finalScale + bitmapWidth / 2
    val cropBottomInBitmap = (viewHeight - bitmapCenterYInView) / finalScale + bitmapHeight / 2
    
    val cropWidth = cropRightInBitmap - cropLeftInBitmap
    val cropHeight = cropBottomInBitmap - cropTopInBitmap
    
    // Create the matrix for potential rotation (if needed) but here just scaling
    val matrix = Matrix()
    
    // Perform the crop
    return try {
        Bitmap.createBitmap(
            source,
            cropLeftInBitmap.toInt().coerceIn(0, (bitmapWidth - 1).toInt()),
            cropTopInBitmap.toInt().coerceIn(0, (bitmapHeight - 1).toInt()),
            cropWidth.toInt().coerceIn(1, (bitmapWidth - cropLeftInBitmap).toInt()),
            cropHeight.toInt().coerceIn(1, (bitmapHeight - cropTopInBitmap).toInt()),
            matrix,
            true
        )
    } catch (e: Exception) {
        // Fallback to center square if math fails
        val size = minOf(source.width, source.height)
        Bitmap.createBitmap(source, (source.width - size) / 2, (source.height - size) / 2, size, size)
    }
}
