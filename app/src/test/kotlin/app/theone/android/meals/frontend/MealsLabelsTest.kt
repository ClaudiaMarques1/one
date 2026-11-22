package app.theone.android.meals.frontend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class MealsLabelsTest {
    @Test
    fun `macro line uses the handoff format with en-GB grouping`() {
        assertEquals("640 kcal · P 48 · C 70 · F 16", macroLine(640, 48, 70, 16))
        assertEquals("2,500 kcal · P 160 · C 280 · F 80", macroLine(2500, 160, 280, 80))
    }

    @Test
    fun `day title names nearby days, then the weekday`() {
        val today = LocalDate.of(2026, 11, 19)
        assertEquals("Today", dayTitle(today, today))
        assertEquals("Tomorrow", dayTitle(today.plusDays(1), today))
        assertEquals("Yesterday", dayTitle(today.minusDays(1), today))
        assertEquals("Saturday", dayTitle(today.plusDays(2), today, Locale.UK))
    }

    @Test
    fun `today header date`() {
        val today = LocalDate.of(2026, 11, 19)
        // Weekday + date only under Today / Tomorrow / Yesterday; further out the title is the weekday.
        assertEquals("Thursday, 19 Nov", dayDateLabel(today, today, Locale.UK))
        assertEquals("Friday, 20 Nov", dayDateLabel(today.plusDays(1), today, Locale.UK))
        assertEquals("Wednesday, 18 Nov", dayDateLabel(today.minusDays(1), today, Locale.UK))
        assertEquals("21 Nov", dayDateLabel(today.plusDays(2), today, Locale.UK))
        assertEquals("17 Nov", dayDateLabel(today.minusDays(2), today, Locale.UK))
    }

    @Test
    fun `water label in litres without trailing zeros`() {
        assertEquals("1.25 of 2.5 L", waterLabel(5, 10))
        assertEquals("1 of 2.5 L", waterLabel(4, 10))
        assertEquals("0 of 3 L", waterLabel(0, 12))
    }

    @Test
    fun `add button label follows the footer states`() {
        assertEquals("Pick a meal", addButtonLabel("Snacks", picks = 0, newTab = false))
        assertEquals("Add 2 to Snacks", addButtonLabel("Snacks", picks = 2, newTab = false))
        assertEquals("Add to Snacks", addButtonLabel("Snacks", picks = 0, newTab = true))
    }

    @Test
    fun `amount fields - blank is 0, negatives and text are invalid`() {
        assertEquals(0, parseAmount(""))
        assertEquals(24, parseAmount(" 24 "))
        assertNull(parseAmount("-3"))
        assertNull(parseAmount("ten"))
    }
}
