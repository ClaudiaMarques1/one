package app.theone.android.workout.sessions.backend

import app.theone.android.workout.templates.backend.TemplateExercise
import kotlinx.coroutines.flow.Flow

/**
 * Strength session storage. The app uses [FirestoreStrengthRepository]; tests use an in-memory
 * fake, so ViewModel logic is unit-testable without Firebase.
 */
interface StrengthRepository {
    /** Sessions for the signed-in user, newest first; emits an empty list when signed out. */
    fun observeSessions(): Flow<List<StrengthSession>>

    suspend fun addSession(session: StrengthSession)

    /**
     * Saves [session] and, when [templateId] and [templateExercises] are both given, carries the
     * logged exercises back onto that template - atomically, so neither write lands without the other.
     */
    suspend fun addSessionAndUpdateTemplate(
        session: StrengthSession,
        templateId: String?,
        templateExercises: List<TemplateExercise>?
    )
}
