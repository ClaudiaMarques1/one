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
 * Dashed rounded-rect outline ([width], default 1dp) ([dash] 8dp, [gap] 6dp by default - shorter for small
 * shapes like a calendar cell or a legend swatch). Pass a large [cornerRadius] for a pill.
 * calendar/frontend/CalendarScreen.kt still has its own private copy - switch it over when that
 * file's ktlint baseline entries are cleaned up.
 */
fun Modifier.dashedBorder(color: Color, cornerRadius: Dp, width: Dp = 1.dp, dash: Dp = 8.dp, gap: Dp = 6.dp): Modifier =
    this
        .clip(RoundedCornerShape(cornerRadius))
        .drawWithCache {
            val stroke = Stroke(
                width = width.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()), 0f)
            )
            val radius = minOf(cornerRadius.toPx(), size.minDimension / 2)
            onDrawBehind {
                drawRoundRect(color = color, cornerRadius = CornerRadius(radius, radius), style = stroke)
            }
        }
