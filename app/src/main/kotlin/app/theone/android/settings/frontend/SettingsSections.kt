package app.theone.android.settings.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.meals.backend.MealSettings
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.NeutralIconTile
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmMutedIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText

// Settings split into sections (#62): the top level lists them; each opens its own screen.

/**
 * A Settings section - one per app section, in bottom-nav order (Appearance stays on the Settings
 * screen itself, as asked on #62). [key] is its route segment (`settings/{key}`), so don't rename it.
 */
enum class SettingsSection(val key: String, val title: String, val icon: ImageVector) {
    CALENDAR("calendar", "Calendar", Icons.Outlined.CalendarMonth),
    WORKOUT("workout", "Workout", Icons.Outlined.FitnessCenter),
    MEALS("meals", "Meals", Icons.Outlined.Restaurant),
    DIARY("diary", "Diary", Icons.Outlined.Book);

    companion object {
        fun fromKey(key: String?): SettingsSection? = entries.firstOrNull { it.key == key }
    }
}

/** The summary of a section that has nothing to set yet (Calendar, Diary). */
internal const val NO_SETTINGS_YET = "No settings yet"

/** Workout's row summary: "Vibrate: On · Do Not Disturb: Off". */
internal fun workoutSummary(vibrateOnRestEnd: Boolean, dndDuringWorkout: Boolean): String =
    "Vibrate: ${onOff(vibrateOnRestEnd)} · Do Not Disturb: ${onOff(dndDuringWorkout)}"

private fun onOff(value: Boolean) = if (value) "On" else "Off"

/** Meals' row summary: "4 slots · 2.5 L water". */
internal fun mealsSummary(settings: MealSettings): String {
    val slots = settings.enabledSlots.size
    val litres = settings.waterGlasses * 0.25
    val water = if (litres % 1.0 == 0.0) "${litres.toInt()} L" else "$litres L"
    return "${if (slots == 1) "1 slot" else "$slots slots"} · $water water"
}

/** The Settings list: one card per section with its summary; tapping opens it. */
@Composable
internal fun SettingsSectionList(summaries: Map<SettingsSection, String>, onOpen: (SettingsSection) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsSection.entries.forEach { section ->
            SectionRow(section, summaries[section].orEmpty()) { onOpen(section) }
        }
    }
}

@Composable
private fun SectionRow(section: SettingsSection, summary: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardSurface)
            .border(1.dp, WarmCardBorder, shape)
            .clickable(onClickLabel = "Open ${section.title}", onClick = onClick)
            .padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(NeutralIconTile),
            contentAlignment = Alignment.Center
        ) {
            Icon(section.icon, contentDescription = null, tint = WarmOnSurface, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(section.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface)
            Text(summary, fontSize = 13.sp, color = WarmSecondaryText, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = WarmMutedIcon)
    }
}
