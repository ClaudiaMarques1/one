package app.theone.android.diary.goals.backend

import android.content.Context
import app.theone.android.notifications.HabitReminderScheduler

// Keeps WorkManager habit reminders in step with a goal's habits when it is saved or deleted.
// One job per reminder time; jobs left over from per-part-of-day reminders are cancelled too.

internal fun applyReminderScheduling(
    context: Context,
    goalName: String,
    oldHabits: List<Habit>,
    newHabits: List<Habit>
) {
    val newById = newHabits.associateBy { it.id }
    oldHabits.forEach { old ->
        val new = newById[old.id]
        if (new == null) HabitReminderScheduler.cancelLegacy(context, old.id)
        old.reminders
            .filter { reminder -> new?.reminders?.none { it.id == reminder.id } ?: true }
            .forEach { HabitReminderScheduler.cancel(context, old.id, it.id) }
    }

    newHabits.forEach { habit ->
        HabitReminderScheduler.cancelLegacy(context, habit.id)
        habit.reminders.forEach { reminder ->
            if (reminder.enabled) {
                HabitReminderScheduler.schedule(
                    context = context,
                    habitId = habit.id,
                    reminderId = reminder.id,
                    habitName = habit.name,
                    goalName = goalName,
                    timeOfDayLabel = partOf(reminder.hour).label,
                    hour = reminder.hour,
                    minute = reminder.minute
                )
            } else {
                HabitReminderScheduler.cancel(context, habit.id, reminder.id)
            }
        }
    }
}

internal fun cancelAllReminders(context: Context, habits: List<Habit>) {
    habits.forEach { habit ->
        HabitReminderScheduler.cancelLegacy(context, habit.id)
        habit.reminders.forEach { HabitReminderScheduler.cancel(context, habit.id, it.id) }
    }
}
