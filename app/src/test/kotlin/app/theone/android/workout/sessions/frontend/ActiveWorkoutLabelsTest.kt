package app.theone.android.workout.sessions.frontend

import app.theone.android.workout.sessions.backend.StrengthSet
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveWorkoutLabelsTest {
    @Test
    fun `formatElapsed shows mm-ss under an hour`() {
        assertEquals("00:00", formatElapsed(0))
        assertEquals("04:05", formatElapsed(245))
        assertEquals("59:59", formatElapsed(3599))
    }

    @Test
    fun `formatElapsed shows h-mm-ss from one hour`() {
        assertEquals("1:00:00", formatElapsed(3600))
        assertEquals("1:02:03", formatElapsed(3723))
    }

    @Test
    fun `formatElapsed clamps negative time to zero`() {
        assertEquals("00:00", formatElapsed(-5))
    }

    @Test
    fun `formatRestRemaining shows m-ss`() {
        assertEquals("1:24", formatRestRemaining(84))
        assertEquals("0:05", formatRestRemaining(5))
        assertEquals("0:00", formatRestRemaining(-1))
    }

    @Test
    fun `restLabel is Off for zero and m-ss otherwise`() {
        assertEquals("Off", restLabel(0))
        assertEquals("0:30", restLabel(30))
        assertEquals("1:30", restLabel(90))
        assertEquals("3:00", restLabel(180))
    }

    @Test
    fun `previousLabel formats weight, bodyweight and missing sets`() {
        assertEquals("60 kg × 8", previousLabel(StrengthSet(reps = 8, weightKg = 60.0)))
        assertEquals("62.5 kg × 6", previousLabel(StrengthSet(reps = 6, weightKg = 62.5)))
        assertEquals("BW × 12", previousLabel(StrengthSet(reps = 12, weightKg = 0.0)))
        assertEquals("—", previousLabel(null))
    }

    @Test
    fun `progressLabel counts done of total sets`() {
        assertEquals("2 of 13 sets", progressLabel(2, 13))
    }
}
