package com.nkds.hosikoouma.jasmine.ui.components.common

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.circle
import com.nkds.hosikoouma.jasmine.ui.theme.toComposeShape

object ExpressiveShapeProvider {

    fun getShapeById(shapeId: String): RoundedPolygon? {
        return when (shapeId) {
            "CIRCLE" -> RoundedPolygon.circle(numVertices = 40)
            "SQUARE" -> RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.2f))
            "TRIANGLE" -> RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.15f))
            "PENTAGON" -> RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.15f))
            "DIAMOND" -> RoundedPolygon.star(numVerticesPerRadius = 2, innerRadius = 0.5f, rounding = CornerRounding(0.1f))
            "OVAL" -> RoundedPolygon.circle(numVertices = 40)
            
            // Cookie-подобные фигуры
            "COOKIE_4" -> createCookie(4)
            "COOKIE_6" -> createCookie(6)
            "COOKIE_7" -> createCookie(7)
            "COOKIE_9" -> createCookie(9)
            "COOKIE_12" -> createCookie(12)
            
            // Органика
            "HEART" -> createHeart()
            "FLOWER" -> RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.6f, rounding = CornerRounding(0.4f))
            "CLOVER_4" -> RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.3f, rounding = CornerRounding(0.5f))
            "CLOVER_8" -> RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.5f, rounding = CornerRounding(0.3f))
            
            // Игривые
            "SUNNY" -> RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = 0.8f, rounding = CornerRounding(0.1f))
            "BOOM" -> RoundedPolygon.star(numVerticesPerRadius = 15, innerRadius = 0.4f, rounding = CornerRounding(0.05f))
            "SOFT_BOOM" -> RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = 0.6f, rounding = CornerRounding(0.3f))
            "BURST" -> RoundedPolygon.star(numVerticesPerRadius = 20, innerRadius = 0.5f, rounding = CornerRounding(0.02f))
            
            // Фантазийные
            "PUFFY" -> RoundedPolygon.star(numVerticesPerRadius = 6, innerRadius = 0.8f, rounding = CornerRounding(0.5f))
            "GHOSTISH" -> RoundedPolygon.star(numVerticesPerRadius = 3, innerRadius = 0.7f, rounding = CornerRounding(0.4f))
            "BUN" -> RoundedPolygon.star(numVerticesPerRadius = 3, innerRadius = 0.4f, rounding = CornerRounding(0.6f))
            
            else -> RoundedPolygon.circle(numVertices = 40)
        }
    }

    private fun createCookie(sides: Int): RoundedPolygon {
        return RoundedPolygon.star(
            numVerticesPerRadius = sides,
            innerRadius = 0.85f,
            rounding = CornerRounding(radius = 0.2f, smoothing = 0.8f)
        )
    }

    private fun createHeart(): RoundedPolygon {
        // Создаем сердце через ручное задание вершин для правильной формы
        return RoundedPolygon(
            vertices = floatArrayOf(
                0.5f, 1.0f,  // Низ
                1.0f, 0.45f, // Правый бок
                0.8f, 0.05f, // Правое "ушко"
                0.5f, 0.3f,  // Выемка сверху
                0.2f, 0.05f, // Левое "ушко"
                0.0f, 0.45f  // Левый бок
            ),
            rounding = CornerRounding(radius = 0.25f, smoothing = 0.5f)
        )
    }
}

@Composable
fun rememberExpressiveShape(
    shapeId: String,
    fallbackShape: Shape = CircleShape
): Shape {
    return remember(shapeId) {
        ExpressiveShapeProvider.getShapeById(shapeId)?.toComposeShape() ?: fallbackShape
    }
}
