package com.example.one.workout

import com.example.one.data.FirebaseModule
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StrengthRepository {

    private fun sessionsCollection(): CollectionReference? {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return null
        return FirebaseModule.firestore.collection("users").document(uid).collection("strengthSessions")
    }

    fun observeSessions(): Flow<List<StrengthSession>> = callbackFlow {
        val collection = sessionsCollection()
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
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(StrengthSession::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addSession(session: StrengthSession) {
        sessionsCollection()?.add(session)?.await()
    }

    suspend fun deleteSession(id: String) {
        sessionsCollection()?.document(id)?.delete()?.await()
    }
}
