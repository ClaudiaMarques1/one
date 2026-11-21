package app.theone.android.workout.sessions.backend

import org.junit.Assert.assertEquals
import org.junit.Test

class RestVibratorTest {
    @Test
    fun `countdown ticks at 3, 2 and 1 seconds left only`() {
        assertEquals(listOf(3, 2, 1), (0..90).filter(::isRestCountdownTick).sortedDescending())
    }
}
