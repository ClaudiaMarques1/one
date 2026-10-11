package app.theone.android.diary.goals.backend

import android.content.Context
import app.theone.android.notifications.HabitReminderScheduler

// Keeps WorkManager habit reminders in step with a goal's habits when it is saved or deleted.

internal fun applyReminderScheduling(
    context: Context,
    goalName: String,
    oldHabits: List<Habit>,
    newHabits: List<Habit>
) {
    val newHabitIds = newHabits.map { it.id }.toSet()
    oldHabits.filter { it.id !in newHabitIds }.forEach { removedHabit ->
        removedHabit.timesOfDay.forEach { tod ->
            HabitReminderScheduler.cancel(context, removedHabit.id, tod)
        }
    }

    newHabits.forEach { habit ->
        habit.timesOfDay.forEach { tod ->
            val reminder = habit.reminders.find { it.timeOfDay == tod }
            if (reminder != null && reminder.enabled) {
                val timeOfDay = TimeOfDay.valueOf(tod)
                HabitReminderScheduler.schedule(
                    context = context,
                    habitId = habit.id,
                    habitName = habit.name,
                    goalName = goalName,
                    timeOfDay = tod,
                    timeOfDayLabel = timeOfDay.label,
                    hour = reminder.hour,
                    minute = reminder.minute
                )
            } else {
                HabitReminderScheduler.cancel(context, habit.id, tod)
            }
        }
    }
}

internal fun cancelAllReminders(context: Context, habits: List<Habit>) {
    habits.forEach { habit ->
        habit.timesOfDay.forEach { tod ->
            HabitReminderScheduler.cancel(context, habit.id, tod)
        }
    }
}
