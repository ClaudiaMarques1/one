package app.theone.android.workout.sessions.backend

import app.theone.android.data.FirebaseModule
import app.theone.android.workout.templates.backend.TemplateExercise
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreStrengthRepository : StrengthRepository {
    private fun sessionsCollection(): CollectionReference? = FirebaseModule.userCollection("strengthSessions")

    override fun observeSessions(): Flow<List<StrengthSession>> = callbackFlow {
        val collection = sessionsCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(365) // bounds re-read cost as history grows; ~a year of daily sessions
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(StrengthSession::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun addSession(session: StrengthSession) {
        sessionsCollection()?.add(session)?.await()
    }

    // Writes the session and (optionally) the template's carried-over exercises in one Firestore
    // transaction, so a failure partway through leaves neither write applied - see
    // .claude/rules/architecture.md's atomic multi-write note.
    override suspend fun addSessionAndUpdateTemplate(
        session: StrengthSession,
        templateId: String?,
        templateExercises: List<TemplateExercise>?
    ) {
        val sessions = sessionsCollection() ?: return
        FirebaseModule.firestore.runTransaction { transaction ->
            transaction.set(sessions.document(), session)
            if (!templateId.isNullOrEmpty() && templateExercises != null) {
                val templateRef = FirebaseModule.userCollection("workoutTemplates")?.document(templateId)
                if (templateRef != null) {
                    transaction.update(templateRef, "exercises", templateExercises)
                }
            }
        }.await()
    }
}
