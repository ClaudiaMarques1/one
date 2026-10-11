package app.theone.android.calendar

import app.theone.android.workout.StrengthSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarScreenFormattingTest {
    @Test
    fun `formatWeight drops the decimal for whole numbers`() {
        assertEquals("60", formatWeight(60.0))
    }

    @Test
    fun `formatWeight keeps the decimal for fractional values`() {
        assertEquals("62.5", formatWeight(62.5))
    }

    @Test
    fun `weightRangeLabel is null when every set is bodyweight`() {
        assertNull(weightRangeLabel(listOf(0.0, 0.0)))
    }

    @Test
    fun `weightRangeLabel is a single value when all sets match`() {
        assertEquals("60", weightRangeLabel(listOf(60.0, 60.0, 60.0)))
    }

    @Test
    fun `weightRangeLabel is a range when sets differ`() {
        assertEquals("40-60", weightRangeLabel(listOf(60.0, 40.0, 50.0)))
    }

    @Test
    fun `exerciseSummaryLabel shows a single reps count and omits weight at 0kg`() {
        val sets = listOf(StrengthSet(reps = 10, weightKg = 0.0), StrengthSet(reps = 10, weightKg = 0.0))
        assertEquals("2 × 10", exerciseSummaryLabel(sets))
    }

    @Test
    fun `exerciseSummaryLabel shows a reps range when sets vary`() {
        val sets = listOf(StrengthSet(reps = 10, weightKg = 60.0), StrengthSet(reps = 8, weightKg = 60.0))
        assertEquals("2 × 8-10 · 60 kg", exerciseSummaryLabel(sets))
    }

    @Test
    fun `exerciseSummaryLabel includes a single weight when all sets match`() {
        val sets = listOf(StrengthSet(reps = 8, weightKg = 62.5), StrengthSet(reps = 8, weightKg = 62.5))
        assertEquals("2 × 8 · 62.5 kg", exerciseSummaryLabel(sets))
    }
}
