package app.theone.android.diary.journal.frontend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class BodySide { FRONT, BACK }

private val FigureNeutralColor = Color(0xFFECE6E0)
private val FigureIdleColor = Color(0xFFD9D0C8)
private val FigureStrokeColor = Color(0xFFFFFBFE)

/**
 * The athletic front/back figure from design_handoff_diary_journal. Draws [BodyFigureData]'s
 * left-half shapes twice (mirrored around x = 100 in its 200x400 space) and mirrors tapped points
 * the same way for hit testing, so only one Path per shape is ever built.
 */
@Composable
internal fun BodyFigure(
    side: BodySide,
    levelFor: (partId: String) -> Int,
    onTapPart: (partId: String) -> Unit,
    levelColors: List<Color>,
    modifier: Modifier = Modifier,
    width: Dp = 170.dp
) {
    val data = if (side == BodySide.BACK) BodyFigureData.back else BodyFigureData.front
    val headPath = remember { BodyFigureData.head.toComposePath() }
    val neutralPaths = remember(data) { data.neutral.map { it.toComposePath() } }
    val partPaths = remember(data) { data.parts.map { it.id to it.shape.toComposePath() } }

    Canvas(
        modifier = modifier
            .size(width, width * 2f)
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val scaleX = size.width / 200f
                    val scaleY = size.height / 400f
                    if (scaleX <= 0f || scaleY <= 0f) return@detectTapGestures
                    val pointX = offset.x / scaleX
                    val pointY = offset.y / scaleY
                    val testX = if (pointX > 100f) 200f - pointX else pointX
                    for (index in partPaths.indices.reversed()) {
                        val (id, path) = partPaths[index]
                        if (path.containsPoint(testX, pointY)) {
                            onTapPart(id)
                            break
                        }
                    }
                }
            }
            .semantics { contentDescription = "Body map. Use the list below to mark sore areas." }
    ) {
        val scaleX = size.width / 200f
        val scaleY = size.height / 400f
        scale(scaleX = scaleX, scaleY = scaleY, pivot = Offset.Zero) {
            drawPath(headPath, color = FigureNeutralColor)
            neutralPaths.forEach { drawPath(it, color = FigureNeutralColor) }
            partPaths.forEach { (id, path) -> drawRegion(path, colorForLevel(levelFor(id), levelColors)) }
            scale(scaleX = -1f, scaleY = 1f, pivot = Offset(100f, 0f)) {
                neutralPaths.forEach { drawPath(it, color = FigureNeutralColor) }
                partPaths.forEach { (id, path) -> drawRegion(path, colorForLevel(levelFor(id), levelColors)) }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRegion(path: Path, fill: Color) {
    drawPath(path, color = fill)
    drawPath(path, color = FigureStrokeColor, style = Stroke(width = 2.5f, join = StrokeJoin.Round))
}

private fun colorForLevel(level: Int, levelColors: List<Color>): Color =
    if (level in 1..levelColors.size) levelColors[level - 1] else FigureIdleColor

private fun FigureShape.toComposePath(): Path = when (this) {
    is FigureShape.PathData -> PathParser().parsePathString(d).toPath()
    is FigureShape.Ellipse -> Path().apply { addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry)) }
    is FigureShape.RoundRect -> Path().apply {
        addRoundRect(RoundRect(x, y, x + w, y + h, cornerRadius = CornerRadius(r, r)))
    }
}

private fun Path.containsPoint(x: Float, y: Float): Boolean {
    val androidPath = asAndroidPath()
    val bounds = android.graphics.RectF()
    androidPath.computeBounds(bounds, true)
    val clip = android.graphics.Region(
        bounds.left.toInt(),
        bounds.top.toInt(),
        bounds.right.toInt() + 1,
        bounds.bottom.toInt() + 1
    )
    val region = android.graphics.Region()
    region.setPath(androidPath, clip)
    return region.contains(x.toInt(), y.toInt())
}
