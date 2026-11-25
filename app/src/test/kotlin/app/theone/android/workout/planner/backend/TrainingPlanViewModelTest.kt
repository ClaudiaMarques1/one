package app.theone.android.workout.planner.backend

import app.theone.android.settings.backend.FakeSettingsRepository
import app.theone.android.settings.backend.WeekStart
import app.theone.android.testing.MainDispatcherRule
import app.theone.android.workout.sessions.backend.FakeStrengthRepository
import app.theone.android.workout.sessions.backend.StrengthSession
import app.theone.android.workout.templates.backend.FakeWorkoutTemplateRepository
import app.theone.android.workout.templates.backend.WorkoutTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// Today is Tue 13 Oct 2026.
class TrainingPlanViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 13)
    private val templates = FakeWorkoutTemplateRepository(
        listOf(
            WorkoutTemplate(id = "push", name = "Push day"),
            WorkoutTemplate(id = "pull", name = "Pull day"),
            WorkoutTemplate(id = "leg", name = "Leg day")
        )
    )
    private var settings = FakeSettingsRepository()
    private var plan = FakeTrainingPlanRepository()
    private var sessions = FakeStrengthRepository()

    private val viewModel by lazy { TrainingPlanViewModel(settings, plan, templates, sessions, today) }

    // week and sheet are WhileSubscribed, so they only reflect the repositories while collected.
    private fun CoroutineScope.collectAll() {
        launch(mainDispatcherRule.testDispatcher) { viewModel.week.collect {} }
        launch(mainDispatcherRule.testDispatcher) { viewModel.sheet.collect {} }
    }

    private val weekdayRoutine = TrainingRoutine(
        effectiveFrom = "2026-10-05",
        weekdayPlan = mapOf("MONDAY" to WeekdaySlot("push"), "THURSDAY" to WeekdaySlot("leg", "07:30")),
        weeklyTarget = 2
    )

    @Test
    fun `with no routine the week card is loaded but has nothing to show`() = runTest {
        backgroundScope.collectAll()
        assertTrue(viewModel.week.value.loaded)
        assertFalse(viewModel.week.value.hasRoutine)
    }

    @Test
    fun `the week card counts logged days and planned sessions against the target`() = runTest {
        plan = FakeTrainingPlanRepository(routines = listOf(weekdayRoutine))
        sessions = FakeStrengthRepository(listOf(StrengthSession(id = "s1", date = "2026-10-12")))
        backgroundScope.collectAll()

        val week = viewModel.week.value
        assertEquals(TrainingWeek(done = 1, planned = 1, target = 2), week.week)
        assertEquals("1 of 2 done · 1 more planned", week.note)
        assertEquals("By weekday: Mon Push day, Thu Leg day", week.routineLine)
    }

    @Test
    fun `a routine day whose template was deleted is a rest day`() = runTest {
        plan = FakeTrainingPlanRepository(
            routines = listOf(weekdayRoutine.copy(weekdayPlan = mapOf("THURSDAY" to WeekdaySlot("deleted"))))
        )
        backgroundScope.collectAll()

        assertEquals(0, viewModel.week.value.week.planned)
        assertEquals("By weekday: no training days", viewModel.week.value.routineLine)
        viewModel.openSheet()
        assertEquals("Your routine has 0 days. Add 2 more to reach it.", viewModel.sheet.value?.targetNote)
    }

    @Test
    fun `toggling the week card stores the expanded state`() = runTest {
        backgroundScope.collectAll()
        viewModel.toggleWeekExpanded()
        assertTrue(settings.storedTrainingWeekExpanded)
        assertTrue(viewModel.week.value.expanded)
    }

    @Test
    fun `a new plan starts empty and saves as a version from today`() = runTest {
        backgroundScope.collectAll()
        viewModel.openSheet()
        assertEquals("Nothing planned yet", viewModel.sheet.value?.nextUp)

        viewModel.setWeekdayTemplate(DayOfWeek.MONDAY, "push")
        viewModel.setWeekdayTime(DayOfWeek.MONDAY, LocalTime.of(7, 30))
        viewModel.changeTarget(-2)
        assertEquals("Your routine has 1 day a week, which matches.", viewModel.sheet.value?.targetNote)

        var saved = false
        viewModel.save { saved = true }

        assertTrue(saved)
        assertNull(viewModel.sheet.value)
        val stored = plan.storedRoutines.single()
        assertEquals("2026-10-13", stored.effectiveFrom)
        assertEquals(mapOf("MONDAY" to WeekdaySlot("push", "07:30")), stored.weekdayPlan)
        assertEquals(1, stored.weeklyTarget)
        assertTrue(viewModel.week.value.hasRoutine)
    }

    @Test
    fun `the sheet opens on the saved routine and shows what's next`() = runTest {
        plan = FakeTrainingPlanRepository(routines = listOf(weekdayRoutine))
        backgroundScope.collectAll()
        viewModel.openSheet()

        val sheet = viewModel.sheet.value!!
        assertEquals(weekdayRoutine, sheet.draft)
        assertEquals("Next: Leg day · Thu 15 Oct at 07:30", sheet.nextUp)
        assertEquals(listOf("leg", "pull", "push"), sheet.templates.map { it.id })
    }

    @Test
    fun `closing the sheet discards the changes`() = runTest {
        plan = FakeTrainingPlanRepository(routines = listOf(weekdayRoutine))
        backgroundScope.collectAll()
        viewModel.openSheet()
        viewModel.setWeekdayTemplate(DayOfWeek.MONDAY, null)
        viewModel.closeSheet()

        assertNull(viewModel.sheet.value)
        assertEquals(listOf(weekdayRoutine), plan.storedRoutines)
        viewModel.openSheet()
        assertEquals(weekdayRoutine, viewModel.sheet.value?.draft)
    }

    @Test
    fun `switching to a split starts from the weekday plan and the order can be edited`() = runTest {
        plan = FakeTrainingPlanRepository(routines = listOf(weekdayRoutine))
        backgroundScope.collectAll()
        viewModel.openSheet()

        viewModel.setMode(PlanMode.SPLIT)
        var draft = viewModel.sheet.value!!.draft
        assertEquals(listOf("MONDAY", "THURSDAY"), draft.splitDays)
        assertEquals(listOf("push", "leg"), draft.splitOrder)

        viewModel.addSplit() // after "leg" in name order (leg, pull, push) comes "pull"
        viewModel.moveSplitUp(2)
        viewModel.removeSplit(0)
        viewModel.toggleSplitDay(DayOfWeek.FRIDAY)
        viewModel.setSplitTemplate(1, "push")
        draft = viewModel.sheet.value!!.draft
        assertEquals(listOf("pull", "push"), draft.splitOrder)
        assertEquals(listOf("MONDAY", "THURSDAY", "FRIDAY"), draft.splitDays)
    }

    @Test
    fun `the split order is capped at six and keeps at least one`() = runTest {
        backgroundScope.collectAll()
        viewModel.openSheet()
        viewModel.setMode(PlanMode.SPLIT)
        repeat(10) { viewModel.addSplit() }
        assertEquals(MAX_SPLIT, viewModel.sheet.value!!.draft.splitOrder.size)
        assertFalse(viewModel.sheet.value!!.canAddSplit)
        repeat(10) { viewModel.removeSplit(0) }
        assertEquals(1, viewModel.sheet.value!!.draft.splitOrder.size)
    }

    @Test
    fun `saving a changed split counts it from today, an unchanged one keeps its anchor`() = runTest {
        val split = TrainingRoutine(
            effectiveFrom = "2026-10-05",
            mode = PlanMode.SPLIT,
            splitDays = listOf("MONDAY", "THURSDAY"),
            splitOrder = listOf("push", "leg"),
            splitAnchor = "2026-10-05"
        )
        plan = FakeTrainingPlanRepository(routines = listOf(split))
        backgroundScope.collectAll()

        viewModel.openSheet()
        viewModel.changeTarget(1)
        viewModel.save {}
        assertEquals("2026-10-05", plan.storedRoutines.last().splitAnchor)

        viewModel.openSheet()
        viewModel.toggleSplitDay(DayOfWeek.FRIDAY)
        viewModel.save {}
        assertEquals("2026-10-13", plan.storedRoutines.last().splitAnchor)
        // Both saves were today, so the second overwrote the first: one version from 5 Oct, one from today.
        assertEquals(listOf("2026-10-05", "2026-10-13"), plan.storedRoutines.map { it.effectiveFrom })
    }

    @Test
    fun `a split without days is not saved`() = runTest {
        backgroundScope.collectAll()
        viewModel.openSheet()
        viewModel.setMode(PlanMode.SPLIT)
        viewModel.save {}
        assertEquals("Pick at least one training day and one workout for the split.", viewModel.planError.value)
        assertTrue(plan.storedRoutines.isEmpty())
        assertNotNull(viewModel.sheet.value)
    }

    @Test
    fun `a failed save keeps the sheet open and shows the error`() = runTest {
        backgroundScope.collectAll()
        viewModel.openSheet()
        plan.failNext = IllegalStateException("offline")
        var saved = false
        viewModel.save { saved = true }

        assertFalse(saved)
        assertEquals("offline", viewModel.planError.value)
        assertNotNull(viewModel.sheet.value)
        viewModel.clearPlanError()
        assertNull(viewModel.planError.value)
    }

    private fun CoroutineScope.collectDays() {
        launch(mainDispatcherRule.testDispatcher) { viewModel.monthStates.collect {} }
        launch(mainDispatcherRule.testDispatcher) { viewModel.dayEditor.collect {} }
    }

    private fun thu15() = viewModel.dayEditor.value!!.session!!

    @Test
    fun `the month shows planned, missed and skipped days`() = runTest {
        plan = FakeTrainingPlanRepository(
            routines = listOf(weekdayRoutine),
            overrides = listOf(DayOverride("2026-10-22", skipped = true))
        )
        sessions = FakeStrengthRepository(listOf(StrengthSession(id = "s1", date = "2026-10-05")))
        backgroundScope.collectAll()
        backgroundScope.collectDays()

        val states = viewModel.monthStates.value
        // Mon 5 was done (not listed), Thu 8 and Mon 12 missed, Thu 15 on planned, Thu 22 skipped.
        assertNull(states["2026-10-05"])
        assertEquals(DayState.MISSED, states["2026-10-08"])
        assertEquals(DayState.MISSED, states["2026-10-12"])
        assertEquals(DayState.PLANNED, states["2026-10-15"])
        assertEquals(DayState.SKIPPED, states["2026-10-22"])
        assertNull(states["2026-10-14"])
    }

    @Test
    fun `changing a day's workout and time saves one override, and Reset removes it`() = runTest {
        plan = FakeTrainingPlanRepository(routines = listOf(weekdayRoutine))
        backgroundScope.collectAll()
        backgroundScope.collectDays()
        viewModel.openDay(LocalDate.of(2026, 10, 15))

        viewModel.setDayTemplate(thu15(), "pull")
        viewModel.stepDayTime(thu15(), 30)
        assertEquals(listOf(DayOverride("2026-10-15", templateId = "pull", time = "08:00")), plan.storedOverrides)
        assertEquals("Changed for this day only", viewModel.dayEditor.value?.status)

        viewModel.resetDay(thu15())
        assertTrue(plan.storedOverrides.isEmpty())
        assertEquals("From your routine", viewModel.dayEditor.value?.status)
    }

    @Test
    fun `moving a day follows the session, and moving it back clears the change`() = runTest {
        plan = FakeTrainingPlanRepository(routines = listOf(weekdayRoutine))
        backgroundScope.collectAll()
        backgroundScope.collectDays()
        viewModel.openDay(LocalDate.of(2026, 10, 15))

        viewModel.moveDay(thu15(), LocalDate.of(2026, 10, 17))
        assertEquals(LocalDate.of(2026, 10, 17), viewModel.dayEditor.value?.date)
        assertEquals("Moved from Thu 15 Oct", viewModel.dayEditor.value?.status)
        assertEquals("2026-10-17", plan.storedOverrides.single().moveTo)

        viewModel.moveDay(viewModel.dayEditor.value!!.session!!, LocalDate.of(2026, 10, 15))
        assertTrue(plan.storedOverrides.isEmpty())
        assertEquals("From your routine", viewModel.dayEditor.value?.status)
    }

    @Test
    fun `skip and undo leave no override behind`() = runTest {
        plan = FakeTrainingPlanRepository(routines = listOf(weekdayRoutine))
        backgroundScope.collectAll()
        backgroundScope.collectDays()
        viewModel.openDay(LocalDate.of(2026, 10, 15))

        viewModel.skipDay(thu15())
        assertEquals(EditorKind.SKIPPED, viewModel.dayEditor.value?.kind)
        viewModel.undoSkip(thu15())
        assertTrue(plan.storedOverrides.isEmpty())
        assertEquals(EditorKind.PLANNED, viewModel.dayEditor.value?.kind)
    }

    @Test
    fun `plan a workout on a rest day, then remove it`() = runTest {
        plan = FakeTrainingPlanRepository(routines = listOf(weekdayRoutine))
        backgroundScope.collectAll()
        backgroundScope.collectDays()
        val wed = LocalDate.of(2026, 10, 14)
        viewModel.openDay(wed)
        assertEquals(EditorKind.REST, viewModel.dayEditor.value?.kind)

        viewModel.planWorkout(wed)
        val added = viewModel.dayEditor.value!!
        assertEquals("Added for this day only", added.status)
        assertEquals("leg", added.session?.templateId) // first template by name
        assertEquals("Remove", added.resetLabel)

        viewModel.resetDay(added.session!!)
        assertEquals(EditorKind.REST, viewModel.dayEditor.value?.kind)
    }

    @Test
    fun `the Upcoming tab lists the next two weeks and a row toggles the editor`() = runTest {
        plan = FakeTrainingPlanRepository(routines = listOf(weekdayRoutine))
        backgroundScope.collectAll()
        backgroundScope.collectDays()
        viewModel.openSheet()
        viewModel.setTab(PlanTab.UPCOMING)

        val sheet = viewModel.sheet.value!!
        assertEquals(PlanTab.UPCOMING, sheet.tab)
        assertEquals(listOf(15, 19, 22, 26), sheet.upcoming.map { it.day })
        viewModel.toggleUpcomingDay(LocalDate.of(2026, 10, 19))
        assertEquals(LocalDate.of(2026, 10, 19), viewModel.dayEditor.value?.date)
        viewModel.toggleUpcomingDay(LocalDate.of(2026, 10, 19))
        assertNull(viewModel.dayEditor.value)
    }

    @Test
    fun `the weekday rows follow the week start setting`() = runTest {
        settings = FakeSettingsRepository(weekStart = WeekStart.SUNDAY)
        backgroundScope.collectAll()
        viewModel.openSheet()
        assertEquals(DayOfWeek.SUNDAY, viewModel.sheet.value?.weekDays?.first())
    }
}
