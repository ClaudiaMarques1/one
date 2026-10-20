package app.theone.android.diary.goals.backend

import java.util.UUID

// Reminder-time rules for the goal builder: parsing typed times, the 15-minute dropdown steps,
// the "Add time" default, and bringing reminders saved before per-time ids into the current shape.

/** Minutes past midnight, the sort key for a habit's reminder rows. */
val HabitReminder.minuteOfDay: Int get() = hour * 60 + minute

/** Dropdown options: every 15 minutes from 00:00 to 23:45, as minutes past midnight (96 items). */
val quarterHourOptions: List<Int> = (0 until 24 * 60 step 15).toList()

/**
 * Lenient time input, matching Google Calendar: "8", "8:47", "847", "847p", "8:47 PM", "12a",
 * "20:47". Spaces are ignored and case doesn't matter. Without am/pm the hour is 24-hour.
 * Returns (hour, minute), or null when the input isn't a valid time.
 */
fun parseTime(raw: String): Pair<Int, Int>? {
    val s = raw.trim().lowercase().replace(" ", "")
    val m = Regex("""^(\d{1,2})(?::?(\d{2}))?(a|p|am|pm)?$""").matchEntire(s) ?: return null
    var h = m.groupValues[1].toInt()
    val min = m.groupValues[2].ifEmpty { "0" }.toInt()
    val ap = m.groupValues[3]
    if (min > 59 || h > 23) return null
    if (ap.isNotEmpty()) {
        if (h !in 1..12) return null
        h = h % 12 + if (ap.startsWith("p")) 12 else 0
    }
    return h to min
}

fun List<HabitReminder>.sortedByTime(): List<HabitReminder> = sortedBy { it.minuteOfDay }

/**
 * The time "Add time" appends: 2 hours after the latest one (capped at 23:xx) with the same
 * minutes, or 9:00 when there are none yet. Always starts with its reminder on.
 */
fun nextReminderTime(existing: List<HabitReminder>, newId: () -> String = { UUID.randomUUID().toString() }): HabitReminder {
    val last = existing.maxByOrNull { it.minuteOfDay }
        ?: return HabitReminder(id = newId(), hour = 9, minute = 0, enabled = true)
    return HabitReminder(id = newId(), hour = minOf(last.hour + 2, 23), minute = last.minute, enabled = true)
}

/**
 * Habits saved before per-time reminders had one reminder per part of the day (keyed by
 * `timeOfDay`, minute 0) and no ids. Those become one reminder per `timesOfDay` entry - the saved
 * one if present, else that part's start hour with the reminder off - with a stable
 * `legacy-<PART>` id. Current data passes through, sorted by time.
 */
fun Habit.withMigratedReminders(): Habit {
    if (reminders.isNotEmpty() && reminders.all { it.id.isNotEmpty() }) {
        return copy(reminders = reminders.sortedByTime())
    }
    val migrated = timesOfDay.mapNotNull { tod ->
        val part = TimeOfDay.entries.find { it.name == tod } ?: return@mapNotNull null
        val saved = reminders.find { it.timeOfDay == tod }
        (saved ?: HabitReminder(hour = part.startHour, minute = 0, enabled = false))
            .copy(id = "legacy-$tod", timeOfDay = tod)
    }
    return copy(reminders = migrated.sortedByTime())
}

/** The shape written to Firestore: reminders sorted, each tagged with its part of the day, and `timesOfDay` derived. */
fun Habit.forSave(): Habit {
    val tagged = reminders.sortedByTime().map { it.copy(timeOfDay = partOf(it.hour).name) }
    return copy(reminders = tagged, timesOfDay = tagged.map { it.timeOfDay }.distinct())
}
