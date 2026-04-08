package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.nkds.hosikoouma.jasmine.datamodels.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onSwipeAction: () -> Unit, // Универсальное действие (добавить или удалить)
    onClick: () -> Unit,
    isManualMarkingEnabled: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val threshold = 0.35f
    
    val isAddedToQueue = track.isManual
    var isThresholdReached by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd && isThresholdReached) {
                onSwipeAction()
                isThresholdReached = false
            }
            false
        },
        positionalThreshold = { totalDistance -> totalDistance * threshold }
    )

    val currentProgress = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
        dismissState.progress
    } else 0f

    val reached = currentProgress >= threshold

    LaunchedEffect(reached) {
        if (reached) {
            if (!isThresholdReached) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isThresholdReached = true
            }
        } else {
            isThresholdReached = false
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            // Если трек в очереди - красим в красный (удаление), иначе в основной цвет (добавление)
            val activeColor = if (isAddedToQueue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            
            val bgColor by animateColorAsState(
                if (isThresholdReached) activeColor.copy(alpha = 0.4f)
                else activeColor.copy(alpha = 0.15f),
                label = "swipeBg"
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = if (isAddedToQueue) Icons.Default.PlaylistRemove else Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .graphicsLayer {
                            val scale = if (isThresholdReached) 1.4f else 1.0f
                            scaleX = scale
                            scaleY = scale
                        },
                    tint = if (isThresholdReached) activeColor else activeColor.copy(alpha = 0.6f)
                )
            }
        }
    ) {
        TrackCard(
            track = track,
            isCurrent = isCurrent,
            isManual = if (isManualMarkingEnabled) track.isManual else false,
            isPlaying = isPlaying,
            onClick = onClick,
            trailingContent = trailingContent
        )
    }
}
