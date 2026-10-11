package app.theone.android.calendar.backend

import androidx.lifecycle.SavedStateHandle
import app.theone.android.diary.journal.backend.DiaryEntry
import app.theone.android.diary.journal.backend.FakeDiaryRepository
import app.theone.android.testing.MainDispatcherRule
import app.theone.android.workout.sessions.backend.FakeStrengthRepository
import app.theone.android.workout.sessions.backend.StrengthSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CalendarViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val strengthRepository = FakeStrengthRepository()
    private val diaryRepository = FakeDiaryRepository()

    // Lazy: the ViewModel must be built after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy {
        CalendarViewModel(
            strengthRepository = strengthRepository,
            diaryRepository = diaryRepository,
            savedStateHandle = SavedStateHandle()
        )
    }

    // dayByDate is WhileSubscribed, so it only reflects the repositories while something collects it.
    private fun CoroutineScope.collectDayByDate() {
        launch(mainDispatcherRule.testDispatcher) { viewModel.dayByDate.collect {} }
    }

    @Test
    fun `defaults to Month view on today`() {
        assertEquals(CalendarView.MONTH, viewModel.view.value)
        assertEquals(CalendarDateKeys.todayKey(), viewModel.selectedDate.value)
    }

    @Test
    fun `dayByDate starts empty`() {
        assertTrue(viewModel.dayByDate.value.isEmpty())
    }

    @Test
    fun `calendarError starts null and stays null after clearing`() {
        assertNull(viewModel.calendarError.value)
        viewModel.clearCalendarError()
        assertNull(viewModel.calendarError.value)
    }

    @Test
    fun `setView switches the active view without touching the selected date`() {
        viewModel.selectDate("2026-10-05")

        viewModel.setView(CalendarView.WEEK)

        assertEquals(CalendarView.WEEK, viewModel.view.value)
        assertEquals("2026-10-05", viewModel.selectedDate.value)
    }

    @Test
    fun `selectDate updates the selected date`() {
        viewModel.selectDate("2026-10-05")
        assertEquals("2026-10-05", viewModel.selectedDate.value)
    }

    @Test
    fun `goToToday resets the selected date but keeps the current view`() {
        viewModel.setView(CalendarView.DAY)
        viewModel.selectDate("2020-01-01")

        viewModel.goToToday()

        assertEquals(CalendarDateKeys.todayKey(), viewModel.selectedDate.value)
        assertEquals(CalendarView.DAY, viewModel.view.value)
    }

    @Test
    fun `stepPrevious and stepNext move by month in Month view`() {
        viewModel.selectDate("2026-10-10")
        viewModel.setView(CalendarView.MONTH)

        viewModel.stepNext()
        assertEquals("2026-11-10", viewModel.selectedDate.value)

        viewModel.stepPrevious()
        viewModel.stepPrevious()
        assertEquals("2026-09-10", viewModel.selectedDate.value)
    }

    @Test
    fun `stepPrevious and stepNext move by 7 days in Week view`() {
        viewModel.selectDate("2026-10-10")
        viewModel.setView(CalendarView.WEEK)

        viewModel.stepNext()
        assertEquals("2026-10-17", viewModel.selectedDate.value)

        viewModel.stepPrevious()
        viewModel.stepPrevious()
        assertEquals("2026-10-03", viewModel.selectedDate.value)
    }

    @Test
    fun `stepPrevious and stepNext move by 1 day in Day view`() {
        viewModel.selectDate("2026-10-10")
        viewModel.setView(CalendarView.DAY)

        viewModel.stepNext()
        assertEquals("2026-10-11", viewModel.selectedDate.value)

        viewModel.stepPrevious()
        viewModel.stepPrevious()
        assertEquals("2026-10-09", viewModel.selectedDate.value)
    }

    @Test
    fun `DayActivity is active with a session, a diary entry, or both, but not when empty`() {
        assertFalse(DayActivity().isActive)
        assertTrue(DayActivity(sessions = listOf(StrengthSession(templateName = "Push Day"))).isActive)
        assertTrue(DayActivity(diary = DiaryEntry(mood = "😀")).isActive)
    }

    @Test
    fun `dayByDate merges sessions and diary entries by date`() = runTest {
        strengthRepository.addSession(StrengthSession(date = "2026-10-01", templateName = "Push Day"))
        strengthRepository.addSession(StrengthSession(date = "2026-10-03", templateName = "Legs"))
        diaryRepository.addEntry(DiaryEntry(date = "2026-10-02", mood = "🙂"))
        diaryRepository.addEntry(DiaryEntry(date = "2026-10-03", mood = "💪"))
        backgroundScope.collectDayByDate()

        val days = viewModel.dayByDate.value
        assertEquals(setOf("2026-10-01", "2026-10-02", "2026-10-03"), days.keys)
        assertEquals(listOf("Push Day"), days.getValue("2026-10-01").sessions.map { it.templateName })
        assertNull(days.getValue("2026-10-01").diary)
        assertTrue(days.getValue("2026-10-02").sessions.isEmpty())
        assertEquals("🙂", days.getValue("2026-10-02").diary?.mood)
        assertEquals(listOf("Legs"), days.getValue("2026-10-03").sessions.map { it.templateName })
        assertEquals("💪", days.getValue("2026-10-03").diary?.mood)
    }

    @Test
    fun `dayByDate keeps every session on a date`() = runTest {
        strengthRepository.addSession(StrengthSession(date = "2026-10-01", templateName = "Push Day", createdAt = 1L))
        strengthRepository.addSession(StrengthSession(date = "2026-10-01", templateName = "Pull Day", createdAt = 2L))
        backgroundScope.collectDayByDate()

        assertEquals(
            setOf("Push Day", "Pull Day"),
            viewModel.dayByDate.value.getValue("2026-10-01").sessions.map { it.templateName }.toSet()
        )
    }

    @Test
    fun `dayByDate shows the most recently created diary entry when a date has several`() = runTest {
        diaryRepository.addEntry(DiaryEntry(date = "2026-10-01", mood = "😴", createdAt = 1L))
        diaryRepository.addEntry(DiaryEntry(date = "2026-10-01", mood = "😄", createdAt = 3L))
        diaryRepository.addEntry(DiaryEntry(date = "2026-10-01", mood = "😐", createdAt = 2L))
        backgroundScope.collectDayByDate()

        assertEquals("😄", viewModel.dayByDate.value.getValue("2026-10-01").diary?.mood)
    }

    @Test
    fun `dayByDate updates when a repository changes`() = runTest {
        backgroundScope.collectDayByDate()
        assertTrue(viewModel.dayByDate.value.isEmpty())

        strengthRepository.addSession(StrengthSession(date = "2026-10-05", templateName = "Push Day"))

        assertTrue(viewModel.dayByDate.value.getValue("2026-10-05").isActive)
    }
}
