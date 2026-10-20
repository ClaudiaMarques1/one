package app.theone.android.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class HabitReminderSchedulerTest {
    private val zone = ZoneId.of("Europe/London")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)

    @Test
    fun `reminder later today fires after the remaining time`() {
        val delay = HabitReminderScheduler.computeInitialDelayMillis(9, 30, now = at(2026, 6, 15, 8, 0))
        assertEquals(TimeUnit.MINUTES.toMillis(90), delay)
    }

    @Test
    fun `reminder time already passed rolls to tomorrow`() {
        val delay = HabitReminderScheduler.computeInitialDelayMillis(8, 0, now = at(2026, 6, 15, 9, 0))
        assertEquals(TimeUnit.HOURS.toMillis(23), delay)
    }

    @Test
    fun `reminder exactly now rolls to tomorrow`() {
        val delay = HabitReminderScheduler.computeInitialDelayMillis(8, 0, now = at(2026, 6, 15, 8, 0))
        assertEquals(TimeUnit.HOURS.toMillis(24), delay)
    }

    @Test
    fun `keeps the wall-clock time across a DST change`() {
        // UK clocks go forward at 01:00 on 2026-03-29, so 21:00 -> 08:00 next day is 10 real hours.
        val delay = HabitReminderScheduler.computeInitialDelayMillis(8, 0, now = at(2026, 3, 28, 21, 0))
        assertEquals(TimeUnit.HOURS.toMillis(10), delay)
    }
}
