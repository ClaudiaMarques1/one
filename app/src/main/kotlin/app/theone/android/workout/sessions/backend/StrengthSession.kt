package app.theone.android.workout.sessions.backend

import java.io.Serializable

// Serializable so an in-progress session can be stored in a ViewModel's SavedStateHandle
// (see StrengthViewModel) and survive process death.
data class StrengthSet(
    val reps: Int = 0,
    val weightKg: Double = 0.0
) : Serializable

data class StrengthExercise(
    val name: String = "",
    val sets: List<StrengthSet> = emptyList()
) : Serializable

data class StrengthSession(
    val id: String = "",
    val date: String = "",
    val templateName: String = "",
    val exercises: List<StrengthExercise> = emptyList(),
    /** When the workout ended (saved on Finish). */
    val createdAt: Long = System.currentTimeMillis(),
    /** When the workout started; null for sessions saved before it was recorded (#48). */
    val startedAt: Long? = null,
    /** The workout's notes, written during it or in Edit sets (#48). */
    val notes: String = ""
)
