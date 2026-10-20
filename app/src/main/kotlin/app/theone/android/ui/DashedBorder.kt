package app.theone.android.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 1dp dashed rounded-rect outline (8dp dash, 6dp gap). Pass a large [cornerRadius] for a pill.
 * calendar/frontend/CalendarScreen.kt still has its own private copy - switch it over when that
 * file's ktlint baseline entries are cleaned up.
 */
fun Modifier.dashedBorder(color: Color, cornerRadius: Dp): Modifier =
    this
        .clip(RoundedCornerShape(cornerRadius))
        .drawWithCache {
            val stroke = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), 0f)
            )
            val radius = minOf(cornerRadius.toPx(), size.minDimension / 2)
            onDrawBehind {
                drawRoundRect(color = color, cornerRadius = CornerRadius(radius, radius), style = stroke)
            }
        }
