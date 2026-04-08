package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    isPlaying: Boolean, // Добавляем состояние воспроизведения
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val progress = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    
    // Плавная анимация амплитуды: если пауза - затухает в 0
    val targetAmplitude = if (isPlaying) 25f else 0f
    val animatedAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "amplitude"
    )

    // Анимация фазы (только если играет)
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp) // Увеличиваем высоту для высокой амплитуды
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val newValue = (offset.x / size.width) * (valueRange.endInclusive - valueRange.start) + valueRange.start
                        onValueChange(newValue.coerceIn(valueRange))
                        onValueChangeFinished()
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val deltaProgress = dragAmount / size.width
                        val deltaValue = deltaProgress * (valueRange.endInclusive - valueRange.start)
                        onValueChange((value + deltaValue).coerceIn(valueRange))
                    },
                    onDragEnd = { onValueChangeFinished() }
                )
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val frequency = 12f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val progressWidth = width * progress
            val centerY = height / 2
            
            // 1. Неактивная часть - ровная линия
            drawLine(
                color = inactiveColor,
                start = Offset(progressWidth, centerY),
                end = Offset(width, centerY),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )

            // 2. Активная часть - волна
            if (progress > 0f) {
                val points = 100
                val activePath = Path()
                val activePoints = (points * progress).toInt()
                
                for (i in 0..activePoints) {
                    val x = (i.toFloat() / points) * width
                    // Волна зависит от фазы (движение) и амплитуды (пауза/игра)
                    val y = centerY + (sin(i.toFloat() / (frequency / 10f) + phase) * animatedAmplitude)
                    if (i == 0) activePath.moveTo(x, y) else activePath.lineTo(x, y)
                }
                
                // Конечная точка волны в месте прогресса
                val xEnd = progressWidth
                val yEnd = centerY + (sin((progress * points) / (frequency / 10f) + phase) * animatedAmplitude)
                activePath.lineTo(xEnd, yEnd)
                
                drawPath(
                    path = activePath,
                    color = activeColor,
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )
                
                // 3. Ползунок
                drawCircle(
                    color = activeColor,
                    radius = 8.dp.toPx(),
                    center = Offset(xEnd, yEnd)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = 3.dp.toPx(),
                    center = Offset(xEnd, yEnd)
                )
            }
        }
    }
}
