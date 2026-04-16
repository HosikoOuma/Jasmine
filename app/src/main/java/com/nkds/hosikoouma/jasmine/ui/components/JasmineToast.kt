package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.nkds.hosikoouma.jasmine.datamodels.Track
import kotlinx.coroutines.delay

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

    LaunchedEffect(toastData) {
        if (toastData != null) {
            currentToast = toastData
            isVisible = true
            delay(2500)
            isVisible = false
            delay(400)
            currentToast = null
            onDismiss()
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
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                val data = currentToast!!
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
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                            // Иконка для системных сообщений (удаление)
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
                                color = contentColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
