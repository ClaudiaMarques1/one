package app.theone.android.meals.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.MealsColors
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText

/**
 * Water as a row of glasses (0.25 L each, handoff 1a) - used by the weekly view instead of 1b's -/+
 * buttons. Tapping glass n fills up to it; tapping the last full glass empties it.
 */
@Composable
internal fun WaterCard(glasses: Int, targetGlasses: Int, onTapGlass: (Int) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardSurface)
            .border(1.dp, WarmCardBorder, shape)
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Water", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface, modifier = Modifier.weight(1f))
            Text(waterLabel(glasses, targetGlasses), fontSize = 13.sp, color = WarmSecondaryText)
        }
        // One button per glass of the target; extra glasses past the target still show.
        val count = maxOf(targetGlasses, glasses)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            for (n in 1..count) {
                val full = n <= glasses
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "Set water to ${litres(n)} L") { onTapGlass(n) }
                        .semantics { contentDescription = "Glass $n of $count, ${if (full) "full" else "empty"}" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (full) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        tint = if (full) MealsColors.Eaten else WarmButtonOutline,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
