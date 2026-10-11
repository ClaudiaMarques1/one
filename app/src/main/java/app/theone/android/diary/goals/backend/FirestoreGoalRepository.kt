package app.theone.android.diary.goals.backend

import app.theone.android.data.FirebaseModule
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreGoalRepository : GoalRepository {
    private fun goalsCollection(): CollectionReference? = FirebaseModule.userCollection("goals")

    override fun observeGoals(): Flow<List<Goal>> = callbackFlow {
        val collection = goalsCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100) // goals are a curated set, not a growing log, but bound defensively
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val goals = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Goal::class.java)?.let { goal ->
                        goal.copy(id = doc.id, habits = goal.habits.map { it.withMigratedReminders() })
                    }
                } ?: emptyList()
                trySend(goals)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun addGoal(goal: Goal) {
        goalsCollection()?.add(goal)?.await()
    }

    override suspend fun updateGoal(goal: Goal) {
        goalsCollection()?.document(goal.id)?.set(goal)?.await()
    }

    override suspend fun deleteGoal(id: String) {
        goalsCollection()?.document(id)?.delete()?.await()
    }
}
