package app.theone.android.meals.backend

/** Meal slots in checklist order (meals handoff). Stored by [name], so don't rename the entries. */
enum class MealSlot(val label: String, val optional: Boolean = false) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACKS("Snacks"),
    PRE_WORKOUT("Pre-workout", optional = true),
    POST_WORKOUT("Post-workout", optional = true);

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

/** Settings -> Meals. Kept on the device (SettingsRepository), not in Firestore. */
data class MealSettings(
    val enabledSlots: Set<MealSlot> = setOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACKS),
    val trainingTargets: MealTargets = DEFAULT_TRAINING_TARGETS,
    val restTargets: MealTargets = DEFAULT_REST_TARGETS,
    val waterGlasses: Int = DEFAULT_WATER_GLASSES
)

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
