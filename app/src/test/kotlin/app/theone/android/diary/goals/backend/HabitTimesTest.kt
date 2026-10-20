package app.theone.android.diary.goals.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HabitTimesTest {
    @Test
    fun `parseTime accepts the handover's lenient inputs`() {
        val cases = mapOf(
            "8" to (8 to 0),
            "8:47" to (8 to 47),
            "847" to (8 to 47),
            "847p" to (20 to 47),
            "8:47pm" to (20 to 47),
            "8:47 PM" to (20 to 47),
            "12a" to (0 to 0),
            "12p" to (12 to 0),
            "20:47" to (20 to 47),
            " 7:30 am " to (7 to 30)
        )
        cases.forEach { (input, expected) -> assertEquals("input \"$input\"", expected, parseTime(input)) }
    }

    @Test
    fun `parseTime rejects invalid inputs`() {
        listOf("25:00", "8:75", "13pm", "abc", "", "0a", "8:4").forEach { input ->
            assertNull("input \"$input\"", parseTime(input))
        }
    }

    @Test
    fun `partOf maps hours to the handover's ranges`() {
        assertEquals(TimeOfDay.NIGHT, partOf(4))
        assertEquals(TimeOfDay.MORNING, partOf(5))
        assertEquals(TimeOfDay.MORNING, partOf(11))
        assertEquals(TimeOfDay.AFTERNOON, partOf(12))
        assertEquals(TimeOfDay.AFTERNOON, partOf(17))
        assertEquals(TimeOfDay.EVENING, partOf(18))
        assertEquals(TimeOfDay.EVENING, partOf(20))
        assertEquals(TimeOfDay.NIGHT, partOf(21))
        assertEquals(TimeOfDay.NIGHT, partOf(0))
    }

    @Test
    fun `quarterHourOptions has 96 steps from midnight to 23 45`() {
        assertEquals(96, quarterHourOptions.size)
        assertEquals(0, quarterHourOptions.first())
        assertEquals(23 * 60 + 45, quarterHourOptions.last())
    }

    @Test
    fun `nextReminderTime starts at 9 AM, then adds 2 hours to the latest, capped at 23`() {
        val first = nextReminderTime(emptyList()) { "id" }
        assertEquals(HabitReminder(id = "id", hour = 9, minute = 0, enabled = true), first)

        val existing = listOf(HabitReminder(hour = 7, minute = 30), HabitReminder(hour = 13, minute = 15))
        assertEquals(15 to 15, nextReminderTime(existing).let { it.hour to it.minute })

        val late = listOf(HabitReminder(hour = 22, minute = 45))
        assertEquals(23 to 45, nextReminderTime(late).let { it.hour to it.minute })
    }

    @Test
    fun `withMigratedReminders turns per-part reminders into stable-id times`() {
        val old = Habit(
            id = "h1",
            timesOfDay = listOf("EVENING", "MORNING"),
            reminders = listOf(HabitReminder(timeOfDay = "MORNING", hour = 7, minute = 0, enabled = true))
        )

        val migrated = old.withMigratedReminders().reminders

        assertEquals(
            listOf(
                HabitReminder(id = "legacy-MORNING", timeOfDay = "MORNING", hour = 7, minute = 0, enabled = true),
                HabitReminder(id = "legacy-EVENING", timeOfDay = "EVENING", hour = 18, minute = 0, enabled = false)
            ),
            migrated
        )
        // Same ids every read, so WorkManager names stay stable until the habit is re-saved.
        assertEquals(migrated, old.withMigratedReminders().reminders)
    }

    @Test
    fun `withMigratedReminders leaves current data alone apart from sorting`() {
        val current = Habit(reminders = listOf(HabitReminder(id = "b", hour = 20), HabitReminder(id = "a", hour = 6)))

        assertEquals(listOf("a", "b"), current.withMigratedReminders().reminders.map { it.id })
    }

    @Test
    fun `forSave sorts reminders, tags each with its part, and derives timesOfDay`() {
        val habit = Habit(
            reminders = listOf(
                HabitReminder(id = "c", hour = 19),
                HabitReminder(id = "a", hour = 7),
                HabitReminder(id = "b", hour = 8, enabled = false)
            )
        )

        val saved = habit.forSave()

        assertEquals(listOf("a", "b", "c"), saved.reminders.map { it.id })
        assertEquals(listOf("MORNING", "MORNING", "EVENING"), saved.reminders.map { it.timeOfDay })
        assertEquals(listOf("MORNING", "EVENING"), saved.timesOfDay)
    }
}
