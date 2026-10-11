package app.theone.android.diary.journal.frontend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DiaryEntryComposerTest {
    @Test
    fun `cycleSorenessLevel moves an unmarked part to Mild`() {
        assertEquals(mapOf("quads" to 1), cycleSorenessLevel(emptyMap(), "quads"))
    }

    @Test
    fun `cycleSorenessLevel steps Mild to Sore to Very sore`() {
        var soreness = cycleSorenessLevel(emptyMap(), "quads")
        soreness = cycleSorenessLevel(soreness, "quads")
        assertEquals(2, soreness["quads"])
        soreness = cycleSorenessLevel(soreness, "quads")
        assertEquals(3, soreness["quads"])
    }

    @Test
    fun `cycleSorenessLevel clears the part after Very sore`() {
        val soreness = cycleSorenessLevel(mapOf("quads" to 3), "quads")
        assertFalse(soreness.containsKey("quads"))
    }

    @Test
    fun `cycleSorenessLevel leaves other parts untouched`() {
        val soreness = cycleSorenessLevel(mapOf("back" to 2), "quads")
        assertEquals(mapOf("back" to 2, "quads" to 1), soreness)
    }

    @Test
    fun `clampSleepHours stays within 0 to 14`() {
        assertEquals(0, clampSleepHours(-1))
        assertEquals(14, clampSleepHours(15))
        assertEquals(7, clampSleepHours(7))
    }

    @Test
    fun `sorenessCount counts only the given ids`() {
        val soreness = mapOf("quads" to 1, "shoulders" to 2, "chest" to 3)
        assertEquals(2, sorenessCount(soreness, listOf("quads", "shoulders", "calves")))
    }
}
