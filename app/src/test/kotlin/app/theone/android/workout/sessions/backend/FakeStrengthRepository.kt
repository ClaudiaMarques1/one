package app.theone.android.workout.sessions.backend

import app.theone.android.workout.templates.backend.TemplateExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [StrengthRepository] for tests. Sessions come back newest `createdAt` first and
 * `addSession`/`addSessionAndUpdateTemplate` assign an id. Template carry-overs are recorded in
 * [templateUpdates] rather than applied anywhere - the Firestore transaction's atomicity isn't
 * modelled, only which writes were requested.
 *
 * Set [failNext] to make the next write throw it, to exercise a ViewModel's error path.
 */
class FakeStrengthRepository(initial: List<StrengthSession> = emptyList()) : StrengthRepository {
    private val sessions = MutableStateFlow(initial)
    private var nextId = 1

    var failNext: Throwable? = null

    /** Current stored sessions, in insertion order. */
    val stored: List<StrengthSession> get() = sessions.value

    /** Every template carry-over requested, as (templateId, exercises). */
    val templateUpdates = mutableListOf<Pair<String, List<TemplateExercise>>>()

    override fun observeSessions(): Flow<List<StrengthSession>> =
        sessions.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun addSession(session: StrengthSession) {
        throwIfFailing()
        store(session)
    }

    override suspend fun updateSession(session: StrengthSession) {
        throwIfFailing()
        sessions.value = sessions.value.map { if (it.id == session.id) session else it }
    }

    override suspend fun deleteSession(id: String) {
        throwIfFailing()
        sessions.value = sessions.value.filterNot { it.id == id }
    }

    override suspend fun addSessionAndUpdateTemplate(
        session: StrengthSession,
        templateId: String?,
        templateExercises: List<TemplateExercise>?
    ) {
        throwIfFailing()
        store(session)
        if (!templateId.isNullOrEmpty() && templateExercises != null) {
            templateUpdates += templateId to templateExercises
        }
    }

    private fun store(session: StrengthSession) {
        sessions.value = sessions.value + session.copy(id = "fake-${nextId++}")
    }

    private fun throwIfFailing() {
        failNext?.let {
            failNext = null
            throw it
        }
    }
}
