package app.theone.android.workout.templates.backend

data class TemplateSet(
    val reps: Int = 0,
    val weightKg: Double = 0.0
)

data class TemplateExercise(
    val name: String = "",
    val sets: List<TemplateSet> = emptyList()
)

data class WorkoutTemplate(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val exercises: List<TemplateExercise> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
