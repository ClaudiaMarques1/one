package app.theone.android.diary.goals.backend

enum class TimeOfDay(val label: String, val startHour: Int, val endHour: Int) {
    MORNING("Morning", 5, 12),
    AFTERNOON("Afternoon", 12, 18),
    EVENING("Evening", 18, 21),
    NIGHT("Night", 21, 5)
}

data class HabitReminder(
    val timeOfDay: String = "",
    val hour: Int = 0,
    val minute: Int = 0,
    val enabled: Boolean = true
)

data class Habit(
    val id: String = "",
    val name: String = "",
    val timesOfDay: List<String> = emptyList(),
    val reminders: List<HabitReminder> = emptyList()
)

data class Goal(
    val id: String = "",
    val name: String = "",
    val habits: List<Habit> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
