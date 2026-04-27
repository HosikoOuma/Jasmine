package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nkds.hosikoouma.jasmine.ui.components.common.rememberExpressiveShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val fraction = state.distanceFraction.coerceIn(0f, 1f)
    
    // Теперь всегда используется 12-гранная форма
    val shapeId = "COOKIE_12"
    
    val shape = rememberExpressiveShape(shapeId)

    // Анимация вращения
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val refreshingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(42.dp)
                .graphicsLayer {
                    // Вращаем либо по свайпу, либо бесконечно при обновлении
                    rotationZ = if (isRefreshing) refreshingRotation else fraction * 360f
                    // Эффект появления
                    scaleX = 0.8f + (fraction * 0.2f)
                    scaleY = 0.8f + (fraction * 0.2f)
                    alpha = if (isRefreshing) 1f else fraction.coerceIn(0f, 1f)
                }
                .clip(shape),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 6.dp,
            shadowElevation = 3.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
