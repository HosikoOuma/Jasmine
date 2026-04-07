package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
    onSwipeToAdd: () -> Unit,
    onClick: () -> Unit,
    isManualMarkingEnabled: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val threshold = 0.35f // Оптимальный порог: 35% ширины экрана
    
    // Используем переменную для фиксации того, достигли ли мы физической точки
    var isThresholdReached by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            // Добавляем в очередь только если палец реально дошел до порога.
            // Это игнорирует быстрые "флики" (смахивания), которые не достигли 35% ширины.
            if (it == SwipeToDismissBoxValue.StartToEnd && isThresholdReached) {
                onSwipeToAdd()
                isThresholdReached = false // Сбрасываем после срабатывания
            }
            false // Всегда возвращаем карточку в исходное положение
        },
        positionalThreshold = { totalDistance -> totalDistance * threshold }
    )

    // В M3 progress идет от 0.0 до 1.0 относительно ВСЕЙ ширины компонента
    // Но если задан positionalThreshold, то в confirmValueChange он учитывается.
    // Для визуализации и виброотклика используем расчет относительно ширины.
    val currentProgress = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
        dismissState.progress
    } else 0f

    // Считаем порог пройденным, когда протащили на 35% (threshold)
    val reached = currentProgress >= threshold

    LaunchedEffect(reached) {
        if (reached) {
            if (!isThresholdReached) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isThresholdReached = true
            }
        } else {
            // Сбрасываем состояние "достигнуто", если пользователь отвел палец назад (менее 15% ширины)
            if (currentProgress < 0.15f) {
                isThresholdReached = false
            }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            val bgColor by animateColorAsState(
                if (isThresholdReached) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
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
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .graphicsLayer {
                            // Иконка увеличивается при достижении порога
                            val scale = if (isThresholdReached) 1.4f else 1.0f
                            scaleX = scale
                            scaleY = scale
                        },
                    tint = if (isThresholdReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
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
