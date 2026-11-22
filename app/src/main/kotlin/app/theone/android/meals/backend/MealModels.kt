package app.theone.android.meals.backend

import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Meal slots in checklist order (meals handoff). Stored by [name], so don't rename the entries.
 * [defaultMinutes] is the handoff's default time (minutes after midnight), used to order the timeline.
 */
enum class MealSlot(val label: String, val defaultMinutes: Int, val optional: Boolean = false) {
    BREAKFAST("Breakfast", 8 * 60),
    LUNCH("Lunch", 13 * 60),
    DINNER("Dinner", 20 * 60),
    SNACKS("Snacks", 16 * 60),
    PRE_WORKOUT("Pre-workout", 17 * 60 + 15, optional = true),
    POST_WORKOUT("Post-workout", 19 * 60 + 15, optional = true);

    companion object {
        fun fromStored(name: String): MealSlot? = entries.firstOrNull { it.name == name }
    }
}

/** Daily targets: kcal and grams of protein, carbs and fat. */
data class MealTargets(
    val kcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0
)

val DEFAULT_TRAINING_TARGETS = MealTargets(kcal = 2500, proteinG = 160, carbsG = 280, fatG = 80)
val DEFAULT_REST_TARGETS = MealTargets(kcal = 2300, proteinG = 150, carbsG = 250, fatG = 75)

/** One glass is 0.25 L; the default target is 2.5 L. */
const val DEFAULT_WATER_GLASSES = 10
const val MAX_WATER_GLASSES = 20

/** First day of the week strip. Stored by name. */
enum class WeekStart(val dayOfWeek: DayOfWeek) {
    MONDAY(DayOfWeek.MONDAY),
    SUNDAY(DayOfWeek.SUNDAY);

    companion object {
        /** Sunday where the locale starts weeks on Sunday (e.g. en-US), Monday otherwise - as Calendar does. */
        fun fromLocale(locale: Locale = Locale.getDefault()): WeekStart =
            if (WeekFields.of(locale).firstDayOfWeek == DayOfWeek.SUNDAY) SUNDAY else MONDAY
    }
}

val DEFAULT_SLOT_TIMES: Map<MealSlot, Int> = MealSlot.entries.associateWith { it.defaultMinutes }

/** Settings -> Meals. Kept on the device (SettingsRepository), not in Firestore. */
data class MealSettings(
    val enabledSlots: Set<MealSlot> = setOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACKS),
    val trainingTargets: MealTargets = DEFAULT_TRAINING_TARGETS,
    val restTargets: MealTargets = DEFAULT_REST_TARGETS,
    val waterGlasses: Int = DEFAULT_WATER_GLASSES,
    /** Minutes after midnight per slot; orders the timeline. */
    val slotTimes: Map<MealSlot, Int> = DEFAULT_SLOT_TIMES,
    val weekStart: WeekStart = WeekStart.fromLocale()
) {
    fun timeOf(slot: MealSlot): Int = slotTimes[slot] ?: slot.defaultMinutes
}

/**
 * A meal planned into a slot on a date (`users/{uid}/meals`). The meal's values are copied in rather
 * than referenced, so changing or deleting a favourite never rewrites past days.
 */
data class PlannedMeal(
    val id: String = "",
    val date: String = "",
    val slot: String = MealSlot.SNACKS.name,
    val name: String = "",
    val kcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val eaten: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** A saved meal to pick from in the Add sheet (`users/{uid}/favourites`). */
data class FavouriteMeal(
    val id: String = "",
    val name: String = "",
    val kcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/** Glasses of water drunk on a date (`users/{uid}/water/{date}`). */
data class WaterLog(val glasses: Int = 0)

/**
 * The user's choice for a date (`users/{uid}/mealDays/{date}`): [restDay] true marks a rest day, false a
 * training day. No document means automatic - a day with a logged workout is a training day.
 */
data class MealDayChoice(val restDay: Boolean? = null)
