package com.nkds.hosikoouma.jasmine.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: Dp = 6.dp
): Modifier {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration),
        label = "scrollbarAlpha"
    )

    val color = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()

    return this
        .pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown()
                if (down.position.x > size.width - 40.dp.toPx()) {
                    verticalDrag(down.id) { change ->
                        change.consume()
                        val totalItems = state.layoutInfo.totalItemsCount
                        if (totalItems > 0) {
                            val ratio = (change.position.y / size.height).coerceIn(0f, 1f)
                            val targetIndex = (ratio * totalItems).toInt().coerceIn(0, totalItems - 1)
                            scope.launch {
                                state.scrollToItem(targetIndex)
                            }
                        }
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()

            val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
            val needDrawScrollbar = state.isScrollInProgress || alpha > 0.0f

            if (needDrawScrollbar && firstVisibleElementIndex != null) {
                val elementCount = state.layoutInfo.totalItemsCount
                val scrollbarFullHeight = size.height

                if (elementCount <= state.layoutInfo.visibleItemsInfo.size) return@drawWithContent

                val scrollbarHeight = (scrollbarFullHeight / elementCount) * state.layoutInfo.visibleItemsInfo.size
                val scrollbarOffsetY = (scrollbarFullHeight / elementCount) * firstVisibleElementIndex

                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width - width.toPx(), scrollbarOffsetY),
                    size = Size(width.toPx(), scrollbarHeight),
                    alpha = alpha,
                    cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
                )
            }
        }
}

@Composable
fun Modifier.gridVerticalScrollbar(
    state: LazyGridState,
    width: Dp = 6.dp
): Modifier {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration),
        label = "scrollbarAlpha"
    )

    val color = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()

    return this
        .pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown()
                if (down.position.x > size.width - 40.dp.toPx()) {
                    verticalDrag(down.id) { change ->
                        change.consume()
                        val totalItems = state.layoutInfo.totalItemsCount
                        if (totalItems > 0) {
                            val ratio = (change.position.y / size.height).coerceIn(0f, 1f)
                            val targetIndex = (ratio * totalItems).toInt().coerceIn(0, totalItems - 1)
                            scope.launch {
                                state.scrollToItem(targetIndex)
                            }
                        }
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()

            val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
            val needDrawScrollbar = state.isScrollInProgress || alpha > 0.0f

            if (needDrawScrollbar && firstVisibleElementIndex != null) {
                val elementCount = state.layoutInfo.totalItemsCount
                val scrollbarFullHeight = size.height

                if (elementCount <= state.layoutInfo.visibleItemsInfo.size) return@drawWithContent

                val scrollbarHeight = (scrollbarFullHeight / elementCount) * state.layoutInfo.visibleItemsInfo.size
                val scrollbarOffsetY = (scrollbarFullHeight / elementCount) * firstVisibleElementIndex

                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width - width.toPx(), scrollbarOffsetY),
                    size = Size(width.toPx(), scrollbarHeight),
                    alpha = alpha,
                    cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
                )
            }
        }
}
