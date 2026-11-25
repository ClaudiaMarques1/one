package app.theone.android.workout.planner.backend

/*
 * Training planner data (#74), stored per user in Firestore. Dates are ISO `yyyy-MM-dd`, times `HH:mm`,
 * weekdays `java.time.DayOfWeek.name`, workouts `WorkoutTemplate.id` - TrainingSchedule parses them.
 * Handover: OneApp-designs/Calendar/design_handoff_training_planner/README.md "Data model".
 */

const val DEFAULT_TRAINING_TIME = "18:00"

enum class PlanMode { WEEKDAY, SPLIT }

/** How long before a planned session the reminder fires (used from ticket 2). */
enum class ReminderOffset { OFF, MIN_30, HOUR_1, HOUR_2 }

data class WeekdaySlot(
    val templateId: String = "",
    val time: String = DEFAULT_TRAINING_TIME
)

/**
 * One version of the routine. A new version starts on [effectiveFrom] (also its document id), so editing
 * the routine changes today and later while past days keep the routine they had.
 */
data class TrainingRoutine(
    val effectiveFrom: String = "",
    val mode: PlanMode = PlanMode.WEEKDAY,
    /** WEEKDAY mode: keyed by `DayOfWeek.name`; a missing day is a rest day. */
    val weekdayPlan: Map<String, WeekdaySlot> = emptyMap(),
    /** SPLIT mode: the training days (`DayOfWeek.name`). */
    val splitDays: List<String> = emptyList(),
    /** SPLIT mode: template ids taken in turn, A to the end then again (1-6). */
    val splitOrder: List<String> = emptyList(),
    val splitTime: String = DEFAULT_TRAINING_TIME,
    /** SPLIT mode: where the cycle starts counting; blank = [effectiveFrom]. */
    val splitAnchor: String = "",
    val weeklyTarget: Int = 3,
    val reminder: ReminderOffset = ReminderOffset.HOUR_1,
    val nudgeIfNotStarted: Boolean = true,
    val syncGoogleCalendar: Boolean = false
)

/**
 * A one-off change to a single day, keyed by the session's ORIGINAL date ([sourceDate], also the
 * document id). Null fields keep the routine's value.
 */
data class DayOverride(
    val sourceDate: String = "",
    val templateId: String? = null,
    val time: String? = null,
    val moveTo: String? = null,
    val skipped: Boolean = false,
    /** A workout planned on a rest day. */
    val added: Boolean = false
)
