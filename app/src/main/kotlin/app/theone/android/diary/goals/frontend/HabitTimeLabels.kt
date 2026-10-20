package app.theone.android.diary.goals.frontend

import androidx.compose.ui.graphics.Color
import app.theone.android.diary.goals.backend.HabitReminder
import app.theone.android.diary.goals.backend.TimeOfDay
import app.theone.android.ui.theme.AfternoonLabelBackground
import app.theone.android.ui.theme.AfternoonLabelText
import app.theone.android.ui.theme.EveningLabelBackground
import app.theone.android.ui.theme.EveningLabelText
import app.theone.android.ui.theme.MorningLabelBackground
import app.theone.android.ui.theme.MorningLabelText
import app.theone.android.ui.theme.NightLabelBackground
import app.theone.android.ui.theme.NightLabelText
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Display text and colours for habit reminder times.

/** "7:30 AM" in 12-hour locales, "07:30" in 24-hour ones (pass DateFormat.is24HourFormat). */
internal fun formatReminderTime(hour: Int, minute: Int, is24Hour: Boolean): String =
    LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault()))

internal fun HabitReminder.timeLabel(is24Hour: Boolean): String = formatReminderTime(hour, minute, is24Hour)

/** "1 time" / "3 times" in the habit card header. */
internal fun timesCountLabel(count: Int): String = if (count == 1) "1 time" else "$count times"

/** Background and text colour of a part-of-day label. */
internal fun TimeOfDay.labelColors(): Pair<Color, Color> = when (this) {
    TimeOfDay.MORNING -> MorningLabelBackground to MorningLabelText
    TimeOfDay.AFTERNOON -> AfternoonLabelBackground to AfternoonLabelText
    TimeOfDay.EVENING -> EveningLabelBackground to EveningLabelText
    TimeOfDay.NIGHT -> NightLabelBackground to NightLabelText
}
