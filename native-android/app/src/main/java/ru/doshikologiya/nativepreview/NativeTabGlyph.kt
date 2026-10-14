package ru.doshikologiya.nativepreview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// Original nav icons transcribed from the project root's index.html and
// images/ui/heart.svg (24x24, fill none, line round, stroke width 1.9).
private val HomePath = PathParser()
    .parsePathString("M3.5 10.5 12 3.5l8.5 7v9a1 1 0 0 1-1 1h-5.25V14h-4.5v6.5H4.5a1 1 0 0 1-1-1z").toPath()
private val RecipeLines = PathParser().parsePathString("M9 8h6M9 12h6M9 16h4").toPath()
private val OracleStreak = PathParser().parsePathString(
    "M9 7.75a4.5 4.5 0 0 1 3-1.2M9.25 17.5 7.5 21h9l-1.75-3.5").toPath()
private val HeartPath = PathParser().parsePathString(
    "M12 20.6C10.65 19.55 3.55 15.05 2.55 9.65C1.95 6.4 4.05 3.55 7.25 3.55C9.35 3.55 11.05 4.7 12 6.25C12.95 4.7 14.65 3.55 16.75 3.55C19.95 3.55 22.05 6.4 21.45 9.65C20.45 15.05 13.35 19.55 12 20.6Z").toPath()

@Composable
internal fun NativeTabGlyph(screen: Screen, color: Color) {
    Canvas(Modifier.size(26.dp)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        withTransform({ scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero) }) {
            val pen = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (screen) {
                Screen.Home -> drawPath(HomePath, color, style = pen)
                Screen.Library -> {
                    drawRoundRect(color, topLeft = Offset(5f, 2.75f),
                        size = Size(14f, 18.5f), cornerRadius = CornerRadius(2.5f),
                        style = pen)
                    drawPath(RecipeLines, color, style = pen)
                }
                Screen.Oracle -> {
                    drawCircle(color, radius = 7.25f, center = Offset(12f, 10.5f),
                        style = pen)
                    drawPath(OracleStreak, color, style = pen)
                }
                Screen.Wall -> drawPath(HeartPath, color, style = pen)
                Screen.More -> listOf(5f, 12f, 19f).forEach { x ->
                    drawCircle(color, radius = 1.5f, center = Offset(x, 12f),
                        style = pen)
                }
                else -> Unit
            }
        }
    }
}
