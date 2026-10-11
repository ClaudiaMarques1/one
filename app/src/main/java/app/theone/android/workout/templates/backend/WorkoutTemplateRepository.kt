package app.theone.android.workout.templates.backend

import kotlinx.coroutines.flow.Flow

/**
 * Workout template storage. The app uses [FirestoreWorkoutTemplateRepository]; tests use an
 * in-memory fake, so ViewModel logic is unit-testable without Firebase.
 */
interface WorkoutTemplateRepository {
    /** Templates for the signed-in user, newest first; emits an empty list when signed out. */
    fun observeTemplates(): Flow<List<WorkoutTemplate>>

    suspend fun addTemplate(template: WorkoutTemplate)

    /** Overwrites the template with the same id. */
    suspend fun updateTemplate(template: WorkoutTemplate)

    /** Replaces only the template's exercises, leaving its other fields untouched. */
    suspend fun updateTemplateExercises(templateId: String, exercises: List<TemplateExercise>)

    suspend fun deleteTemplate(id: String)
}
