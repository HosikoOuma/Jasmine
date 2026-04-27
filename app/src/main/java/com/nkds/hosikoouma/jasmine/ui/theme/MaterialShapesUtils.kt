package com.nkds.hosikoouma.jasmine.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

enum class ShapeCategory {
    BASIC, ORGANIC, PLAYFUL, COOKIE, WHIMSICAL, SPECIAL, PIXEL
}

enum class ExpressiveMaterialShape(
    val displayName: String,
    val description: String,
    val category: ShapeCategory
) {
    CIRCLE("Circle", "A perfect circle", ShapeCategory.BASIC),
    SQUARE("Square", "A rounded square", ShapeCategory.BASIC),
    OVAL("Oval", "An oval shape", ShapeCategory.BASIC),
    PILL("Pill", "A pill/capsule shape", ShapeCategory.BASIC),
    DIAMOND("Diamond", "A classic diamond shape", ShapeCategory.BASIC),
    TRIANGLE("Triangle", "A rounded triangle", ShapeCategory.BASIC),
    PENTAGON("Pentagon", "A 5-sided polygon", ShapeCategory.BASIC),

    FLOWER("Flower", "A flower-like shape", ShapeCategory.ORGANIC),
    CLOVER_4_LEAF("Clover 4", "4-leaf clover", ShapeCategory.ORGANIC),
    CLOVER_8_LEAF("Clover 8", "8-leaf clover", ShapeCategory.ORGANIC),
    HEART("Heart", "A romantic heart shape", ShapeCategory.ORGANIC),

    BOOM("Boom", "Explosion shape", ShapeCategory.PLAYFUL),
    SOFT_BOOM("Soft Boom", "Softer explosion", ShapeCategory.PLAYFUL),
    BURST("Burst", "Star burst", ShapeCategory.PLAYFUL),
    SOFT_BURST("Soft Burst", "Softer star burst", ShapeCategory.PLAYFUL),
    SUNNY("Sunny", "Sun shape with rays", ShapeCategory.PLAYFUL),
    VERY_SUNNY("Very Sunny", "Sun with more rays", ShapeCategory.PLAYFUL),

    COOKIE_4("Cookie 4", "4-sided cookie", ShapeCategory.COOKIE),
    COOKIE_6("Cookie 6", "6-sided cookie", ShapeCategory.COOKIE),
    COOKIE_7("Cookie 7", "7-sided cookie", ShapeCategory.COOKIE),
    COOKIE_9("Cookie 9", "9-sided cookie", ShapeCategory.COOKIE),
    COOKIE_12("Cookie 12", "12-sided cookie", ShapeCategory.COOKIE),

    GHOSTISH("Ghostish", "A friendly ghost shape", ShapeCategory.WHIMSICAL),
    PUFFY("Puffy", "A puffy cloud", ShapeCategory.WHIMSICAL),
    PUFFY_DIAMOND("Puffy Diamond", "Puffy diamond shape", ShapeCategory.WHIMSICAL),
    BUN("Bun", "Bun / Pretzel shape", ShapeCategory.WHIMSICAL),
    FAN("Fan", "A fan shape", ShapeCategory.WHIMSICAL),
    ARROW("Arrow", "A directional arrow", ShapeCategory.WHIMSICAL),

    ARCH("Arch", "An arch shape", ShapeCategory.SPECIAL),
    CLAM_SHELL("Clam Shell", "A shell shape", ShapeCategory.SPECIAL),
    GEM("Gem", "A gemstone shape", ShapeCategory.SPECIAL),
    SEMI_CIRCLE("Semi Circle", "Half circle", ShapeCategory.SPECIAL),
    SLANTED("Slanted", "A slanted square", ShapeCategory.SPECIAL),

    PIXEL_CIRCLE("Pixel Circle", "Pixelated circle", ShapeCategory.PIXEL),
    PIXEL_TRIANGLE("Pixel Triangle", "Pixelated triangle", ShapeCategory.PIXEL)
}

enum class ShapeTarget {
    ALBUM_ART, PLAYER_ART, SONG_ART, PLAYLIST_ART, ARTIST_ART, PLAYER_CONTROLS, MINI_PLAYER
}

/**
 * Extension to convert RoundedPolygon to a Compose Shape
 */
fun RoundedPolygon.toComposeShape(): Shape {
    return object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val path = this@toComposeShape.toPath().asComposePath()
            val bounds = path.getBounds()
            
            val matrix = Matrix()
            val scaleX = size.width / (bounds.width.takeIf { it > 0f } ?: 1f)
            val scaleY = size.height / (bounds.height.takeIf { it > 0f } ?: 1f)
            
            val translateX = (size.width - bounds.width * scaleX) / 2f - bounds.left * scaleX
            val translateY = (size.height - bounds.height * scaleY) / 2f - bounds.top * scaleY
            
            matrix.scale(scaleX, scaleY)
            matrix.translate(translateX / scaleX, translateY / scaleY)
            
            val scaledPath = Path()
            scaledPath.addPath(path)
            scaledPath.transform(matrix)
            
            return Outline.Generic(scaledPath)
        }
    }
}
