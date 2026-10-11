package app.theone.android.diary

import app.theone.android.data.FirebaseModule
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreDiaryRepository : DiaryRepository {
    private fun entriesCollection(): CollectionReference? = FirebaseModule.userCollection("diaryEntries")

    override fun observeEntries(): Flow<List<DiaryEntry>> = callbackFlow {
        val collection = entriesCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = collection
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(365) // bounds re-read cost as history grows; ~a year of daily entries
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DiaryEntry::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(entries)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun addEntry(entry: DiaryEntry) {
        entriesCollection()?.add(entry)?.await()
    }

    override suspend fun updateEntry(entry: DiaryEntry) {
        if (entry.id.isEmpty()) return
        entriesCollection()?.document(entry.id)?.set(entry)?.await()
    }

    override suspend fun deleteEntry(id: String) {
        entriesCollection()?.document(id)?.delete()?.await()
    }
}
