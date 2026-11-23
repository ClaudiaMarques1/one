package app.theone.android.workout.sessions.backend

import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.TemplateSet
import app.theone.android.workout.templates.backend.WorkoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveWorkoutTest {
    private val bench = ActiveExercise(
        name = "Bench press",
        sets = listOf(ActiveSet(), ActiveSet()),
        previousSets = listOf(StrengthSet(reps = 8, weightKg = 60.0), StrengthSet(reps = 6, weightKg = 62.5)),
        instanceId = "b"
    )

    @Test
    fun `toActiveExercises gives one empty set per template set, keeping previous values and rest`() {
        val template = WorkoutTemplate(
            exercises = listOf(
                TemplateExercise("Bench press", listOf(TemplateSet(8, 60.0), TemplateSet(6, 62.5)), restSeconds = 120),
                TemplateExercise("Plank")
            )
        )

        val active = template.toActiveExercises { "id" }

        assertEquals(listOf(ActiveSet(), ActiveSet()), active[0].sets)
        assertEquals(listOf(StrengthSet(8, 60.0), StrengthSet(6, 62.5)), active[0].previousSets)
        assertEquals(120, active[0].restSeconds)
        assertTrue(active[1].sets.isEmpty())
    }

    @Test
    fun `parse helpers accept a comma decimal and reject junk`() {
        assertEquals(62.5, parseWeightKg("62,5")!!, 0.0)
        assertEquals(60.0, parseWeightKg(" 60 ")!!, 0.0)
        assertNull(parseWeightKg("abc"))
        assertNull(parseWeightKg("-5"))
        assertEquals(8, parseReps("8"))
        assertNull(parseReps("8.5"))
        assertEquals("60", formatWeightInput(60.0))
        assertEquals("62.5", formatWeightInput(62.5))
    }

    @Test
    fun `ticking a set fills empty fields from the previous set and keeps typed ones`() {
        val typed = bench.copy(sets = listOf(ActiveSet(repsText = "10"), ActiveSet()))

        val ticked = typed.toggleSetDone(0)

        assertEquals(ActiveSet(repsText = "10", weightText = "60", done = true), ticked.sets[0])
        assertEquals(ActiveSet(), ticked.sets[1])
        assertFalse(ticked.toggleSetDone(0).sets[0].done)
    }

    @Test
    fun `withAddedSet copies the last set's values, not done`() {
        val withValues = bench.copy(sets = listOf(ActiveSet("8", "60", done = true)))

        assertEquals(ActiveSet("8", "60", done = false), withValues.withAddedSet().sets.last())
        assertEquals(ActiveSet(), ActiveExercise(name = "Plank").withAddedSet().sets.single())
    }

    @Test
    fun `finishing keeps done sets only, or all sets, falling back to previous values`() {
        val exercises = listOf(
            bench.copy(sets = listOf(ActiveSet("10", "", done = true), ActiveSet())),
            ActiveExercise(name = "Plank", sets = listOf(ActiveSet()), instanceId = "p")
        )

        val doneOnly = exercises.toSessionExercises(FinishMode.DONE_SETS_ONLY)
        assertEquals(listOf(StrengthExercise("Bench press", listOf(StrengthSet(10, 60.0)))), doneOnly)

        val all = exercises.toSessionExercises(FinishMode.ALL_SETS)
        assertEquals(listOf(StrengthSet(10, 60.0), StrengthSet(6, 62.5)), all[0].sets)
        assertEquals(listOf(StrengthSet(0, 0.0)), all[1].sets)
    }

    @Test
    fun `the template keeps every exercise, previous sets where nothing was saved, and rest times`() {
        val exercises = listOf(
            bench.copy(sets = listOf(ActiveSet("10", "65", done = true), ActiveSet()), restSeconds = 120),
            ActiveExercise(name = "Row", previousSets = listOf(StrengthSet(12, 40.0)), sets = listOf(ActiveSet()), restSeconds = 0)
        )

        val template = exercises.toTemplateExercises(FinishMode.DONE_SETS_ONLY)

        assertEquals(TemplateExercise("Bench press", listOf(TemplateSet(10, 65.0)), restSeconds = 120), template[0])
        assertEquals(TemplateExercise("Row", listOf(TemplateSet(12, 40.0)), restSeconds = 0), template[1])
    }

    @Test
    fun `set counts and unfinished detection`() {
        val exercises = listOf(bench.copy(sets = listOf(ActiveSet(done = true), ActiveSet())))

        assertEquals(1, exercises.doneSetCount())
        assertEquals(2, exercises.totalSetCount())
        assertTrue(exercises.hasUnfinishedSets())
        assertFalse(listOf(bench.copy(sets = listOf(ActiveSet(done = true)))).hasUnfinishedSets())
    }

    @Test
    fun `rest counts down by wall clock, rounding up`() {
        val rest = RestState("Bench press", totalSeconds = 90, endsAtMillis = 100_000)

        assertEquals(90, rest.remainingSeconds(10_000))
        assertEquals(1, rest.remainingSeconds(99_500))
        assertEquals(0, rest.remainingSeconds(120_000))
        assertEquals(0.5f, rest.elapsedFraction(55_000), 0.001f)
    }

    @Test
    fun `minus 30 ends a rest with 30s or less left, plus 30 extends and raises the total`() {
        val rest = RestState("Bench press", totalSeconds = 90, endsAtMillis = 100_000)

        assertNull(rest.adjusted(-30, nowMillis = 70_000))
        assertEquals(RestState("Bench press", 90, 70_000), rest.adjusted(-30, nowMillis = 10_000))
        assertEquals(RestState("Bench press", 120, 130_000), rest.adjusted(30, nowMillis = 10_000))
        assertEquals(RestState("Bench press", 90, 130_000), rest.adjusted(30, nowMillis = 90_000))
    }

    private fun exercise(id: String, vararg done: Boolean, doneOrder: Int? = null) =
        ActiveExercise(name = id, sets = done.map { ActiveSet(done = it) }, doneOrder = doneOrder, instanceId = id)

    @Test
    fun `hasDoneSet needs at least one set ticked`() {
        assertTrue(exercise("a", true, false).hasDoneSet)
        assertFalse(exercise("a", false, false).hasDoneSet)
        assertFalse(exercise("a").hasDoneSet)
    }

    @Test
    fun `withDoneOrder numbers exercises in the order they are started`() {
        val first = listOf(exercise("a", false, false), exercise("b", true, false), exercise("c", false)).withDoneOrder()
        assertEquals(listOf(null, 1, null), first.map { it.doneOrder })

        val second = first.map { if (it.instanceId == "c") it.copy(sets = listOf(ActiveSet(done = true))) else it }.withDoneOrder()
        assertEquals(listOf(null, 1, 2), second.map { it.doneOrder })
    }

    @Test
    fun `withDoneOrder waits while that exercise is resting`() {
        val list = listOf(exercise("a", true), exercise("b", false))
        assertEquals(listOf(null, null), list.withDoneOrder(restingId = "a").map { it.doneOrder })
        assertEquals(listOf(1, null), list.withDoneOrder(restingId = "b").map { it.doneOrder })
    }

    @Test
    fun `withDoneOrder clears an exercise with no ticked set any more`() {
        val list = listOf(exercise("a", false, false, doneOrder = 1), exercise("b", true, doneOrder = 2)).withDoneOrder()
        assertEquals(listOf(null, 2), list.map { it.doneOrder })
        // Starting "a" again puts it after "b".
        val again = list.map { if (it.instanceId == "a") it.copy(sets = listOf(ActiveSet(done = true))) else it }.withDoneOrder()
        assertEquals(listOf(3, 2), again.map { it.doneOrder })
    }

    @Test
    fun `inDisplayOrder puts done exercises first by finish order, the rest in list order`() {
        val list = listOf(
            exercise("a", false),
            exercise("b", true, doneOrder = 2),
            exercise("c", false),
            exercise("d", true, doneOrder = 1)
        )
        assertEquals(listOf("d", "b", "a", "c"), list.inDisplayOrder().map { it.instanceId })
    }
}
