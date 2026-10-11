package app.theone.android.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules/cancels daily habit reminder notifications via WorkManager. Each (habitId, timeOfDay)
 * pair maps to one unique periodic work request, so re-scheduling with new data simply replaces it.
 */
object HabitReminderScheduler {
    private fun workName(habitId: String, timeOfDay: String) = "habit_reminder_${habitId}_$timeOfDay"

    fun schedule(
        context: Context,
        habitId: String,
        habitName: String,
        goalName: String,
        timeOfDay: String,
        timeOfDayLabel: String,
        hour: Int,
        minute: Int
    ) {
        val data = Data.Builder()
            .putString(HabitReminderWorker.KEY_HABIT_NAME, habitName)
            .putString(HabitReminderWorker.KEY_GOAL_NAME, goalName)
            .putString(HabitReminderWorker.KEY_TIME_OF_DAY_LABEL, timeOfDayLabel)
            .putInt(HabitReminderWorker.KEY_NOTIFICATION_ID, "$habitId-$timeOfDay".hashCode())
            .build()

        val request = PeriodicWorkRequestBuilder<HabitReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(computeInitialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName(habitId, timeOfDay),
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, habitId: String, timeOfDay: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(habitId, timeOfDay))
    }

    // ZonedDateTime keeps the wall-clock time across DST changes, as Calendar did.
    internal fun computeInitialDelayMillis(hour: Int, minute: Int, now: ZonedDateTime = ZonedDateTime.now()): Long {
        val todayAtTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        val target = if (todayAtTime.isAfter(now)) todayAtTime else todayAtTime.plusDays(1)
        return Duration.between(now, target).toMillis()
    }
}
