package app.theone.android.diary.goals.backend

enum class TimeOfDay(val label: String, val startHour: Int, val endHour: Int) {
    MORNING("Morning", 5, 12),
    AFTERNOON("Afternoon", 12, 18),
    EVENING("Evening", 18, 21),
    NIGHT("Night", 21, 5)
}

/** Part of the day a reminder hour falls in; derived, never chosen directly. */
fun partOf(hour: Int): TimeOfDay = when (hour) {
    in 5..11 -> TimeOfDay.MORNING
    in 12..17 -> TimeOfDay.AFTERNOON
    in 18..20 -> TimeOfDay.EVENING
    else -> TimeOfDay.NIGHT
}

data class HabitReminder(
    // Stable per time - part of the WorkManager work name. "" only on data saved before ids existed;
    // withMigratedReminders() fills it in on read.
    val id: String = "",
    // Kept for older readers; derived from `hour` on save.
    val timeOfDay: String = "",
    val hour: Int = 0,
    val minute: Int = 0,
    val enabled: Boolean = true
)

data class Habit(
    val id: String = "",
    val name: String = "",
    // Derived from `reminders` on save, so the goal list and older readers keep working.
    val timesOfDay: List<String> = emptyList(),
    val reminders: List<HabitReminder> = emptyList()
)

data class Goal(
    val id: String = "",
    val name: String = "",
    val habits: List<Habit> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
