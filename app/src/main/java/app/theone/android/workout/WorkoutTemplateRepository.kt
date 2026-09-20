package app.theone.android.workout

import app.theone.android.data.FirebaseModule
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WorkoutTemplateRepository {

    private fun templatesCollection(): CollectionReference? {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return null
        return FirebaseModule.firestore.collection("users").document(uid).collection("workoutTemplates")
    }

    fun observeTemplates(): Flow<List<WorkoutTemplate>> = callbackFlow {
        val collection = templatesCollection()
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
                val templates = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WorkoutTemplate::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(templates)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addTemplate(template: WorkoutTemplate) {
        templatesCollection()?.add(template)?.await()
    }

    suspend fun updateTemplate(template: WorkoutTemplate) {
        templatesCollection()?.document(template.id)?.set(template)?.await()
    }

    suspend fun updateTemplateExercises(templateId: String, exercises: List<TemplateExercise>) {
        templatesCollection()?.document(templateId)?.update("exercises", exercises)?.await()
    }

    suspend fun deleteTemplate(id: String) {
        templatesCollection()?.document(id)?.delete()?.await()
    }
}
