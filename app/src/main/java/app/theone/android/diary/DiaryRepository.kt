package app.theone.android.diary

import kotlinx.coroutines.flow.Flow

/**
 * Diary entry storage. The app uses [FirestoreDiaryRepository]; tests use an in-memory fake, so
 * ViewModel logic is unit-testable without Firebase.
 */
interface DiaryRepository {
    /** Entries for the signed-in user, newest date first; emits an empty list when signed out. */
    fun observeEntries(): Flow<List<DiaryEntry>>

    suspend fun addEntry(entry: DiaryEntry)

    /**
     * Upsert by id - used both for editing an existing entry and for restoring one from a cached
     * copy after a delete-with-undo, so the restored entry keeps its original id. No-op for a blank id.
     */
    suspend fun updateEntry(entry: DiaryEntry)

    suspend fun deleteEntry(id: String)
}
