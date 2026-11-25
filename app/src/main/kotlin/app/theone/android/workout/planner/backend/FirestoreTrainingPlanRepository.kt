package app.theone.android.workout.planner.backend

import app.theone.android.data.FirebaseModule
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** `users/{uid}/trainingRoutines/{effectiveFrom}` and `users/{uid}/trainingOverrides/{sourceDate}`. */
class FirestoreTrainingPlanRepository : TrainingPlanRepository {
    private fun routines(): CollectionReference? = FirebaseModule.userCollection("trainingRoutines")

    private fun overrides(): CollectionReference? = FirebaseModule.userCollection("trainingOverrides")

    override fun observeRoutines(): Flow<List<TrainingRoutine>> = callbackFlow {
        val collection = routines()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = collection
            .orderBy("effectiveFrom", Query.Direction.ASCENDING)
            .limit(500) // one version per edit day - bound defensively
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TrainingRoutine::class.java)?.copy(effectiveFrom = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    override fun observeOverrides(fromDate: String): Flow<List<DayOverride>> = callbackFlow {
        val collection = overrides()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = collection
            .whereGreaterThanOrEqualTo("sourceDate", fromDate)
            .orderBy("sourceDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DayOverride::class.java)?.copy(sourceDate = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun saveRoutine(routine: TrainingRoutine) {
        routines()?.document(routine.effectiveFrom)?.set(routine)?.await()
    }

    override suspend fun saveOverride(override: DayOverride) {
        overrides()?.document(override.sourceDate)?.set(override)?.await()
    }

    override suspend fun deleteOverride(sourceDate: String) {
        overrides()?.document(sourceDate)?.delete()?.await()
    }
}
