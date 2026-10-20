package app.theone.android.diary.journal.backend

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [DiaryRepository] for tests. Mirrors the Firestore implementation's contract: entries
 * come back newest date first, `addEntry` assigns an id, `updateEntry` upserts by id.
 *
 * Set [failNext] to make the next write throw it, to exercise a ViewModel's error path.
 */
class FakeDiaryRepository(initial: List<DiaryEntry> = emptyList()) : DiaryRepository {
    private val entries = MutableStateFlow(initial)
    private var nextId = 1

    var failNext: Throwable? = null

    /** Current stored entries, in insertion order. */
    val stored: List<DiaryEntry> get() = entries.value

    override fun observeEntries(): Flow<List<DiaryEntry>> =
        entries.map { list -> list.sortedByDescending { it.date } }

    override suspend fun addEntry(entry: DiaryEntry) {
        throwIfFailing()
        entries.value = entries.value + entry.copy(id = "fake-${nextId++}")
    }

    override suspend fun updateEntry(entry: DiaryEntry) {
        throwIfFailing()
        if (entry.id.isEmpty()) return
        entries.value = entries.value.filterNot { it.id == entry.id } + entry
    }

    override suspend fun deleteEntry(id: String) {
        throwIfFailing()
        entries.value = entries.value.filterNot { it.id == id }
    }

    private fun throwIfFailing() {
        failNext?.let {
            failNext = null
            throw it
        }
    }
}
