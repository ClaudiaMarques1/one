package app.theone.android.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class HabitReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitName = inputData.getString(KEY_HABIT_NAME) ?: return Result.success()
        val goalName = inputData.getString(KEY_GOAL_NAME) ?: ""
        val timeOfDayLabel = inputData.getString(KEY_TIME_OF_DAY_LABEL) ?: ""
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, habitName.hashCode())

        val text = if (goalName.isNotEmpty()) {
            "$timeOfDayLabel habit for \"$goalName\""
        } else {
            "$timeOfDayLabel habit reminder"
        }

        NotificationHelper.showReminder(applicationContext, notificationId, habitName, text)
        return Result.success()
    }

    companion object {
        const val KEY_HABIT_NAME = "habit_name"
        const val KEY_GOAL_NAME = "goal_name"
        const val KEY_TIME_OF_DAY_LABEL = "time_of_day_label"
        const val KEY_NOTIFICATION_ID = "notification_id"
    }
}
