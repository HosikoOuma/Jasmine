package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nkds.hosikoouma.jasmine.ui.components.common.rememberExpressiveShape

@Composable
fun ShapesSettingsScreen() {
    var isRotating by remember { mutableStateOf(false) }
    
    val allShapes = listOf(
        "COOKIE_4" to "Cookie 4",
        "COOKIE_6" to "Cookie 6",
        "COOKIE_7" to "Cookie 7",
        "COOKIE_9" to "Cookie 9",
        "COOKIE_12" to "Cookie 12",
        "HEART" to "Heart",
        "FLOWER" to "Flower",
        "SUNNY" to "Sunny",
        "PUFFY" to "Puffy",
        "BOOM" to "Boom",
        "SOFT_BOOM" to "Soft Boom",
        "BURST" to "Burst",
        "CLOVER_4" to "Clover 4",
        "CLOVER_8" to "Clover 8",
        "GHOSTISH" to "Ghost",
        "BUN" to "Bun",
        "SQUARE" to "Square",
        "TRIANGLE" to "Triangle",
        "PENTAGON" to "Pentagon",
        "DIAMOND" to "Diamond"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Управление вращением интегрировано в контент
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Organic Animations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Toggle rotation for all shapes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isRotating,
                onCheckedChange = { isRotating = it },
                thumbContent = if (isRotating) {
                    { Icon(Icons.Rounded.Refresh, null, Modifier.size(16.dp)) }
                } else null
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(allShapes) { (id, name) ->
                ShapeCard(id = id, name = name, isRotating = isRotating)
            }
            
            // Отступ снизу для плеера и навигации
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(200.dp))
            }
        }
    }
}

@Composable
fun ShapeCard(id: String, name: String, isRotating: Boolean) {
    val shape = rememberExpressiveShape(id)
    
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .graphicsLayer {
                        if (isRotating) {
                            rotationZ = rotation
                        }
                    }
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
