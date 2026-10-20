package app.theone.android.workout.templates.backend

/** Rest between sets when nothing has been chosen yet; 0 means Off. */
const val DEFAULT_REST_SECONDS = 90

data class TemplateSet(
    val reps: Int = 0,
    val weightKg: Double = 0.0
)

data class TemplateExercise(
    val name: String = "",
    val sets: List<TemplateSet> = emptyList(),
    // Rest between sets for this exercise, remembered from the last workout. 0 = Off.
    val restSeconds: Int = DEFAULT_REST_SECONDS
)

data class WorkoutTemplate(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val exercises: List<TemplateExercise> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
