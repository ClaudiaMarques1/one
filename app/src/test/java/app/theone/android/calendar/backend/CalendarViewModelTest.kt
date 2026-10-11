package app.theone.android.calendar.backend

import androidx.lifecycle.SavedStateHandle
import app.theone.android.diary.journal.backend.DiaryEntry
import app.theone.android.diary.journal.backend.FakeDiaryRepository
import app.theone.android.workout.sessions.backend.StrengthRepository
import app.theone.android.workout.sessions.backend.StrengthSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Covers view/selected-date state management only, which is pure SavedStateHandle mutation -
// StrengthRepository/DiaryRepository are final, Firebase-coupled classes with no seam for a fake,
// so dayByDate's repository-backed merge isn't covered here (see StrengthViewModelTest).
class CalendarViewModelTest {
    private val viewModel = CalendarViewModel(
        strengthRepository = StrengthRepository(),
        diaryRepository = FakeDiaryRepository(),
        savedStateHandle = SavedStateHandle()
    )

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
}
