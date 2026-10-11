package app.theone.android.diary.journal.frontend

import app.theone.android.diary.journal.backend.DiaryEntry
import app.theone.android.diary.journal.backend.FakeDiaryRepository
import app.theone.android.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DiaryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDiaryRepository()

    // Lazy: the ViewModel must be built after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy { DiaryViewModel(repository) }

    @Test
    fun `addEntry saves today's entry with soreness levels and flat part ids`() {
        viewModel.addEntry(
            mood = "🙂",
            energyLevel = 7,
            sleepHours = 8,
            soreness = mapOf("quads" to 3),
            reflection = "Leg day"
        )

        val saved = repository.stored.single()
        assertEquals(DiaryDateUtils.todayKey(), saved.date)
        assertEquals(mapOf("quads" to 3), saved.soreness)
        assertEquals(listOf("quads"), saved.sorePartIds)
        assertEquals("Leg day", saved.reflection)
    }

    @Test
    fun `entries exposes the repository's entries newest first`() = runTest {
        repository.updateEntry(DiaryEntry(id = "a", date = "2026-10-01"))
        repository.updateEntry(DiaryEntry(id = "b", date = "2026-10-05"))
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.entries.collect {} }

        assertEquals(listOf("b", "a"), viewModel.entries.value.map { it.id })
    }

    @Test
    fun `updateEntry replaces the entry with the same id`() {
        viewModel.addEntry("🙂", 5, 7, emptyMap(), "before")
        val original = repository.stored.single()

        viewModel.updateEntry(
            id = original.id,
            date = original.date,
            mood = "💪",
            energyLevel = 9,
            sleepHours = 8,
            soreness = emptyMap(),
            reflection = "after",
            createdAt = original.createdAt
        )

        val updated = repository.stored.single()
        assertEquals(original.id, updated.id)
        assertEquals("after", updated.reflection)
        assertEquals(original.createdAt, updated.createdAt)
    }

    @Test
    fun `delete then restore brings the entry back with its original id`() {
        viewModel.addEntry("🙂", 5, 7, mapOf("back" to 2), "keep me")
        val original = repository.stored.single()

        viewModel.deleteEntry(original.id)
        assertTrue(repository.stored.isEmpty())

        viewModel.restoreEntry(original)
        assertEquals(listOf(original), repository.stored)
    }

    @Test
    fun `a failed write surfaces in diaryError and can be cleared`() {
        repository.failNext = IllegalStateException("offline")

        viewModel.addEntry("🙂", 5, 7, emptyMap(), "")

        assertEquals("offline", viewModel.diaryError.value)
        assertTrue(repository.stored.isEmpty())
        viewModel.clearDiaryError()
        assertNull(viewModel.diaryError.value)
    }
}
