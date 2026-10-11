package app.theone.android.diary.goals.frontend

import app.theone.android.diary.goals.backend.Habit
import app.theone.android.diary.goals.backend.HabitReminder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class HabitTimeLabelsTest {
    private val originalLocale = Locale.getDefault()

    // AM/PM text is locale-specific ("AM" in en-US, "am" in en-GB), so pin it.
    @Before
    fun pinLocale() = Locale.setDefault(Locale.US)

    @After
    fun restoreLocale() = Locale.setDefault(originalLocale)

    @Test
    fun `formatReminderTime uses h mm a in 12-hour and HH mm in 24-hour`() {
        assertEquals("7:30 AM", formatReminderTime(7, 30, is24Hour = false))
        assertEquals("12:00 AM", formatReminderTime(0, 0, is24Hour = false))
        assertEquals("6:45 PM", formatReminderTime(18, 45, is24Hour = false))
        assertEquals("07:30", formatReminderTime(7, 30, is24Hour = true))
        assertEquals("18:45", formatReminderTime(18, 45, is24Hour = true))
    }

    @Test
    fun `timesCountLabel is singular for one`() {
        assertEquals("1 time", timesCountLabel(1))
        assertEquals("3 times", timesCountLabel(3))
    }

    @Test
    fun `goalCardHabitLine lists enabled times in order and leaves out disabled ones`() {
        val habit = Habit(
            name = "Drink water",
            reminders = listOf(
                HabitReminder(id = "c", hour = 18, minute = 45),
                HabitReminder(id = "a", hour = 7, minute = 30),
                HabitReminder(id = "b", hour = 13, minute = 15, enabled = false)
            )
        )

        assertEquals("Drink water · 7:30 AM, 6:45 PM", goalCardHabitLine(habit, is24Hour = false))
    }

    @Test
    fun `goalCardHabitLine is just the name when no time is enabled`() {
        assertEquals("Stretch", goalCardHabitLine(Habit(name = "Stretch"), is24Hour = false))
    }
}
