package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.nkds.hosikoouma.jasmine.core.models.ProgressBarStyle
import kotlin.math.sin

@Composable
fun JasmineProgressBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    style: ProgressBarStyle,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val progress = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    
    val infiniteTransition = rememberInfiniteTransition(label = "progressBar")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) 15f else 0f,
        animationSpec = tween(500),
        label = "amplitude"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(valueRange) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = { onValueChangeFinished() },
                    onDragCancel = { onValueChangeFinished() },
                    onDrag = { change, _ ->
                        change.consume()
                        val width = size.width.toFloat()
                        val newProgress = (change.position.x / width).coerceIn(0f, 1f)
                        val newValue = newProgress * (valueRange.endInclusive - valueRange.start) + valueRange.start
                        onValueChange(newValue)
                    }
                )
            }
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val newProgress = (offset.x / width).coerceIn(0f, 1f)
                    val newValue = newProgress * (valueRange.endInclusive - valueRange.start) + valueRange.start
                    onValueChange(newValue)
                    onValueChangeFinished()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val progressWidth = width * progress

            when (style) {
                ProgressBarStyle.STANDARD -> {
                    drawLine(inactiveColor, Offset(0f, height / 2), Offset(width, height / 2), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(activeColor, Offset(0f, height / 2), Offset(progressWidth, height / 2), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(activeColor, radius = 8.dp.toPx(), center = Offset(progressWidth, height / 2))
                }
                ProgressBarStyle.SOLID -> {
                    drawLine(inactiveColor, Offset(0f, height / 2), Offset(width, height / 2), strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(activeColor, Offset(0f, height / 2), Offset(progressWidth, height / 2), strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round)
                }
                ProgressBarStyle.DOTTED -> {
                    val dotSpacingEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 40f), 0f)
                    val strokeThickness = 10.dp.toPx()
                    
                    drawLine(inactiveColor, Offset(0f, height / 2), Offset(width, height / 2), strokeWidth = strokeThickness, cap = StrokeCap.Round, pathEffect = dotSpacingEffect)
                    drawLine(activeColor, Offset(0f, height / 2), Offset(progressWidth, height / 2), strokeWidth = strokeThickness, cap = StrokeCap.Round, pathEffect = dotSpacingEffect)
                }
                ProgressBarStyle.WAVE -> {
                    val points = 100
                    val frequency = 2f
                    
                    drawLine(inactiveColor, Offset(progressWidth, height / 2), Offset(width, height / 2), strokeWidth = 12f, cap = StrokeCap.Round)

                    val activePath = Path()
                    val activePoints = (points * progress).toInt()
                    
                    if (activePoints >= 0) {
                        for (i in 0..activePoints) {
                            val x = (i.toFloat() / points) * width
                            val y = height / 2 + (sin(i.toFloat() / frequency + phase) * animatedAmplitude)
                            if (i == 0) activePath.moveTo(x, y) else activePath.lineTo(x, y)
                        }
                        
                        val xEnd = progressWidth
                        val yEnd = height / 2 + (sin((progress * points) / frequency + phase) * animatedAmplitude)
                        activePath.lineTo(xEnd, yEnd)
                        
                        drawPath(activePath, activeColor, style = Stroke(width = 14f, cap = StrokeCap.Round))
                        
                        if (progress > 0f) {
                            drawCircle(activeColor, radius = 7.dp.toPx(), center = Offset(xEnd, yEnd))
                        }
                    }
                }
                ProgressBarStyle.NEON -> {
                    drawLine(inactiveColor, Offset(0f, height / 2), Offset(width, height / 2), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(activeColor.copy(alpha = 0.2f), Offset(0f, height / 2), Offset(progressWidth, height / 2), strokeWidth = 12.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(activeColor.copy(alpha = 0.4f), Offset(0f, height / 2), Offset(progressWidth, height / 2), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(activeColor, Offset(0f, height / 2), Offset(progressWidth, height / 2), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
    }
}
