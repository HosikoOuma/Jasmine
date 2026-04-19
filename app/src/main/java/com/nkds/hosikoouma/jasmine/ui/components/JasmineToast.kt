package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.nkds.hosikoouma.jasmine.datamodels.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ToastType {
    ADDED, REMOVED, DELETE_SUCCESS, DELETE_FAILED
}

data class ToastData(
    val track: Track? = null,
    val type: ToastType,
    val message: String? = null
)

@Composable
fun AppToastContainer(
    toastData: ToastData?,
    onDismiss: () -> Unit
) {
    var currentToast by remember { mutableStateOf<ToastData?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(toastData) {
        if (toastData != null) {
            offsetX.snapTo(0f)
            currentToast = toastData
            delay(50) 
            isVisible = true
            delay(2800)
            if (offsetX.value == 0f) {
                isVisible = false
                delay(450) 
                currentToast = null
                onDismiss()
            }
        }
    }

    if (currentToast != null) {
        Popup(
            alignment = Alignment.TopCenter,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .graphicsLayer {
                        translationX = offsetX.value
                        alpha = 1f - (kotlin.math.abs(offsetX.value) / 600f).coerceIn(0f, 1f)
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (kotlin.math.abs(offsetX.value) < 200f) {
                                    scope.launch { offsetX.animateTo(0f, spring()) }
                                } else {
                                    isVisible = false
                                    scope.launch {
                                        offsetX.animateTo(if (offsetX.value > 0) 1000f else -1000f, tween(300))
                                        currentToast = null
                                        onDismiss()
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch { offsetX.animateTo(0f, spring()) }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                            }
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(400, easing = LinearOutSlowInEasing)
                    ) + fadeIn(tween(300)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(400, easing = FastOutLinearInEasing)
                    ) + fadeOut(tween(300))
                ) {
                    ToastContent(currentToast!!)
                }
            }
        }
    }
}

@Composable
private fun ToastContent(data: ToastData) {
    val isError = data.type == ToastType.REMOVED || data.type == ToastType.DELETE_FAILED
    
    val bgColor = when(data.type) {
        ToastType.REMOVED, ToastType.DELETE_FAILED -> MaterialTheme.colorScheme.errorContainer
        ToastType.DELETE_SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
        
    val contentColor = when(data.type) {
        ToastType.REMOVED, ToastType.DELETE_FAILED -> MaterialTheme.colorScheme.onErrorContainer
        ToastType.DELETE_SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = bgColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .widthIn(max = 500.dp)
            .fillMaxWidth()
    ) {
        Box {
            // Прогресс во всю карточку (фон)
            ToastProgressTimer(
                color = contentColor.copy(alpha = 0.12f),
                modifier = Modifier.matchParentSize()
            )

            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (data.track != null) {
                    AlbumArt(
                        albumArtUri = data.track.albumArtUri,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        isLowRes = true
                    )
                } else {
                    Icon(
                        imageVector = if (isError) Icons.Rounded.Error else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).padding(4.dp),
                        tint = contentColor
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    if (data.track != null) {
                        Text(
                            text = data.track.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = data.message ?: when(data.type) {
                            ToastType.REMOVED -> "Removed from Queue"
                            ToastType.ADDED -> "Added to Queue"
                            ToastType.DELETE_SUCCESS -> "Deleted from device"
                            ToastType.DELETE_FAILED -> "Failed to delete"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (data.track == null) FontWeight.Bold else FontWeight.Normal,
                        color = contentColor.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ToastProgressTimer(
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        progress.animateTo(0f, tween(2800, easing = LinearEasing))
    }
    
    Box(
        modifier = modifier.background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(color)
        )
    }
}
