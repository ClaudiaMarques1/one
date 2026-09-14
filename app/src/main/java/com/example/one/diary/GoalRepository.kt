package com.example.one.diary

import com.example.one.data.FirebaseModule
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GoalRepository {

    private fun goalsCollection(): CollectionReference? {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return null
        return FirebaseModule.firestore.collection("users").document(uid).collection("goals")
    }

    fun observeGoals(): Flow<List<Goal>> = callbackFlow {
        val collection = goalsCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val goals = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Goal::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(goals)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addGoal(goal: Goal) {
        goalsCollection()?.add(goal)?.await()
    }

    suspend fun updateGoal(goal: Goal) {
        goalsCollection()?.document(goal.id)?.set(goal)?.await()
    }

    suspend fun deleteGoal(id: String) {
        goalsCollection()?.document(id)?.delete()?.await()
    }
}
