package app.theone.android.meals.frontend

import app.theone.android.meals.backend.DayState
import app.theone.android.meals.backend.ShoppingGroup
import app.theone.android.meals.backend.ShoppingItem
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
    fun `timeline labels follow the handoff`() {
        assertEquals("08:00", formatMinutes(8 * 60, is24Hour = true))
        assertEquals("5:15 PM", formatMinutes(17 * 60 + 15, is24Hour = false, Locale.US))
        assertEquals("1,060 eaten of 2,500 · 900 more planned", timelineSummary(1060, 2500, 900))
        assertEquals("108 g left", gramsLeft(52, 160))
        assertEquals("0 g left", gramsLeft(200, 160))
        assertEquals("November", monthLabel(LocalDate.of(2026, 11, 19), Locale.UK))
        assertEquals("Thu", shortWeekday(LocalDate.of(2026, 11, 19), Locale.UK))
        assertEquals("Plan post-workout", planSlotLabel("Post-workout"))
    }

    @Test
    fun `rest day subtitle explains a locked switch`() {
        assertEquals("Workout logged · training-day targets · 2,500 kcal", restDaySubtitle(rest = false, workoutLogged = true, "2,500"))
        assertEquals("Rest-day targets · 2,300 kcal", restDaySubtitle(rest = true, workoutLogged = false, "2,300"))
        assertEquals("Training-day targets · 2,500 kcal", restDaySubtitle(rest = false, workoutLogged = false, "2,500"))
    }

    @Test
    fun `week plan labels follow the handoff`() {
        assertEquals("15 - 21 Nov", weekRangeLabel(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 21), Locale.UK))
        assertEquals("29 Nov - 5 Dec", weekRangeLabel(LocalDate.of(2026, 11, 29), LocalDate.of(2026, 12, 5), Locale.UK))
        assertEquals("2,280 of 2,300 kcal", weekDaySummary(DayState.PAST, 2280, 2280, 2300))
        assertEquals("1,060 eaten · 1,960 planned", weekDaySummary(DayState.TODAY, 1060, 1960, 2500))
        assertEquals("2,240 kcal planned", weekDaySummary(DayState.FUTURE, 0, 2240, 2300))
        assertEquals("Nothing planned", weekDaySummary(DayState.FUTURE, 0, 0, 2300))
    }

    @Test
    fun `recipe and shopping labels`() {
        assertEquals("520 kcal · P 42 · C 38 · F 20 · 50 min", recipeLine(520, 42, 38, 20, 50))
        assertEquals("340 kcal · P 24 · C 6 · F 24", recipeLine(340, 24, 6, 24, 0))
        assertEquals("250 g", formatQty(250.0, "g"))
        assertEquals("1.5 tins", formatQty(1.5, "tins"))
        assertEquals("0.33", formatQty(1.0 / 3, ""))
        assertEquals("", formatQty(0.0, ""))
        assertEquals(
            "From your plan for 19 - 22 Nov · 12 to buy",
            shoppingSubtitle(LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 22), 12, Locale.UK)
        )
        val groups = listOf(
            ShoppingGroup(
                "Cupboard",
                listOf(
                    ShoppingItem("a", "Kidney beans", 2.0, "tins", "Cupboard", checked = false),
                    ShoppingItem("b", "Rice", 1.0, "kg", "Cupboard", checked = true)
                )
            ),
            ShoppingGroup("Other", listOf(ShoppingItem("m", "Bin bags", 0.0, "", "Other", checked = false, manualIndex = 0)))
        )
        assertEquals("Shopping list\n\nCupboard\n- Kidney beans 2 tins\n\nOther\n- Bin bags", shoppingShareText(groups))
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
