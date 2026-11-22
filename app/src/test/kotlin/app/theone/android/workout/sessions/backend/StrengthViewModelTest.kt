package app.theone.android.workout.sessions.backend

import androidx.lifecycle.SavedStateHandle
import app.theone.android.testing.MainDispatcherRule
import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.TemplateSet
import app.theone.android.workout.templates.backend.WorkoutTemplate
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class StrengthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeStrengthRepository()
    private var now = 1_000_000L

    // Lazy: the ViewModel must be built after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy {
        StrengthViewModel(repository = repository, savedStateHandle = SavedStateHandle(), clock = { now })
    }

    private val pushDay = WorkoutTemplate(
        id = "t1",
        name = "Push Day",
        exercises = listOf(
            TemplateExercise("Bench press", listOf(TemplateSet(8, 60.0), TemplateSet(6, 62.5)), restSeconds = 120),
            TemplateExercise("Lateral raise", listOf(TemplateSet(12, 8.0)), restSeconds = 0)
        )
    )

    private fun bench() = viewModel.currentExercises.value.first { it.name == "Bench press" }

    @Test
    fun `startSessionFromTemplate sets up empty sets with previous values and rest times`() {
        viewModel.startSessionFromTemplate(pushDay)

        assertEquals("Push Day", viewModel.activeTemplateName.value)
        assertEquals(now, viewModel.sessionStartTimeMillis.value)
        assertEquals(listOf(ActiveSet(), ActiveSet()), bench().sets)
        assertEquals(StrengthSet(8, 60.0), bench().previous(0))
        assertEquals(120, bench().restSeconds)
        assertNull(viewModel.restState.value)
    }

    @Test
    fun `discardSession clears the session and any rest`() {
        viewModel.startSessionFromTemplate(pushDay)
        viewModel.toggleSetDone(bench().instanceId, 0)

        viewModel.discardSession()

        assertEquals("", viewModel.activeTemplateName.value)
        assertTrue(viewModel.currentExercises.value.isEmpty())
        assertNull(viewModel.sessionStartTimeMillis.value)
        assertNull(viewModel.restState.value)
    }

    @Test
    fun `typing updates only that set's text`() {
        viewModel.startSessionFromTemplate(pushDay)
        val id = bench().instanceId

        viewModel.updateSetReps(id, 1, "7")
        viewModel.updateSetWeight(id, 1, "62,5")

        assertEquals(ActiveSet(repsText = "7", weightText = "62,5"), bench().sets[1])
        assertEquals(ActiveSet(), bench().sets[0])
    }

    @Test
    fun `ticking a set fills it from previous and starts that exercise's rest`() {
        viewModel.startSessionFromTemplate(pushDay)

        viewModel.toggleSetDone(bench().instanceId, 0)

        assertEquals(ActiveSet("8", "60", done = true), bench().sets[0])
        assertEquals(RestState("Bench press", 120, now + 120_000), viewModel.restState.value)
    }

    @Test
    fun `a new tick replaces the running rest, rest Off starts none, unticking keeps it`() {
        viewModel.startSessionFromTemplate(pushDay)
        viewModel.toggleSetDone(bench().instanceId, 0)
        now += 10_000
        viewModel.toggleSetDone(bench().instanceId, 1)
        assertEquals(now + 120_000, viewModel.restState.value?.endsAtMillis)

        val raise = viewModel.currentExercises.value.first { it.name == "Lateral raise" }
        viewModel.toggleSetDone(raise.instanceId, 0)
        assertEquals("Bench press", viewModel.restState.value?.exerciseName)

        viewModel.toggleSetDone(bench().instanceId, 1)
        assertEquals("Bench press", viewModel.restState.value?.exerciseName)
    }

    @Test
    fun `rest can be adjusted and ended`() {
        viewModel.startSessionFromTemplate(pushDay)
        viewModel.toggleSetDone(bench().instanceId, 0)

        viewModel.adjustRest(30)
        assertEquals(RestState("Bench press", 150, now + 150_000), viewModel.restState.value)

        viewModel.endRest()
        assertNull(viewModel.restState.value)
    }

    @Test
    fun `setRestSeconds is per exercise instance`() {
        viewModel.startSessionFromTemplate(pushDay)
        viewModel.duplicateExercise(bench().instanceId)
        val (original, copy) = viewModel.currentExercises.value.filter { it.name == "Bench press" }

        viewModel.setRestSeconds(copy.instanceId, 30)

        val after = viewModel.currentExercises.value.filter { it.name == "Bench press" }
        assertEquals(listOf(original.instanceId to 120, copy.instanceId to 30), after.map { it.instanceId to it.restSeconds })
    }

    @Test
    fun `duplicateExercise inserts an empty copy right after the original`() {
        viewModel.startSessionFromTemplate(pushDay)

        viewModel.duplicateExercise(bench().instanceId)

        val exercises = viewModel.currentExercises.value
        assertEquals(listOf("Bench press", "Bench press", "Lateral raise"), exercises.map { it.name })
        assertTrue(exercises[1].sets.isEmpty())
        assertEquals(exercises[0].previousSets, exercises[1].previousSets)
    }

    @Test
    fun `addSet copies the last set, removeSet and removeExercise drop by key`() {
        viewModel.startSessionFromTemplate(pushDay)
        val id = bench().instanceId
        viewModel.updateSetReps(id, 1, "6")
        viewModel.updateSetWeight(id, 1, "62.5")

        viewModel.addSet(id)
        assertEquals(ActiveSet("6", "62.5"), bench().sets[2])

        viewModel.removeSet(id, 0)
        assertEquals(2, bench().sets.size)

        viewModel.removeExercise(id)
        assertEquals(listOf("Lateral raise"), viewModel.currentExercises.value.map { it.name })
    }

    @Test
    fun `setExercises keeps picked exercises in place, appends new ones and drops unpicked`() {
        viewModel.startSessionFromTemplate(pushDay)
        viewModel.updateSetReps(bench().instanceId, 0, "9")

        viewModel.setExercises(listOf("Bench press", "Squat"))

        val exercises = viewModel.currentExercises.value
        assertEquals(listOf("Bench press", "Squat"), exercises.map { it.name })
        assertEquals("9", exercises[0].sets[0].repsText)
        assertTrue(exercises[1].sets.isEmpty())
    }

    @Test
    fun `sessions exposes the repository's sessions newest first`() = runTest {
        repository.addSession(StrengthSession(templateName = "old", createdAt = 1L))
        repository.addSession(StrengthSession(templateName = "new", createdAt = 2L))
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.sessions.collect {} }

        assertEquals(listOf("new", "old"), viewModel.sessions.value.map { it.templateName })
    }

    @Test
    fun `finishing an exercise orders it first on screen but the list keeps the template order`() {
        viewModel.startSessionFromTemplate(pushDay)
        val lateral = viewModel.currentExercises.value.first { it.name == "Lateral raise" }
        viewModel.toggleSetDone(lateral.instanceId, 0)

        assertEquals(listOf("Lateral raise", "Bench press"), viewModel.currentExercises.value.inDisplayOrder().map { it.name })
        assertEquals(listOf("Bench press", "Lateral raise"), viewModel.currentExercises.value.map { it.name })

        viewModel.addSet(lateral.instanceId) // a new, unticked set - no longer complete
        assertEquals(listOf("Bench press", "Lateral raise"), viewModel.currentExercises.value.inDisplayOrder().map { it.name })
    }

    @Test
    fun `finishing with done sets only saves today's done sets and carries them back to the template`() {
        viewModel.startSessionFromTemplate(pushDay)
        viewModel.toggleSetDone(bench().instanceId, 0)
        assertTrue(viewModel.hasUnfinishedSets())

        viewModel.finishSession(FinishMode.DONE_SETS_ONLY)

        val saved = repository.stored.single()
        assertEquals(LocalDate.now().toString(), saved.date)
        assertEquals("Push Day", saved.templateName)
        assertEquals(listOf(StrengthExercise("Bench press", listOf(StrengthSet(8, 60.0)))), saved.exercises)
        val (templateId, carried) = repository.templateUpdates.single()
        assertEquals("t1", templateId)
        assertEquals(
            listOf(
                TemplateExercise("Bench press", listOf(TemplateSet(8, 60.0)), restSeconds = 120),
                TemplateExercise("Lateral raise", listOf(TemplateSet(12, 8.0)), restSeconds = 0)
            ),
            carried
        )
        assertEquals("", viewModel.activeTemplateName.value)
    }

    @Test
    fun `finishing with all sets keeps unticked ones too`() {
        viewModel.startSessionFromTemplate(pushDay)
        viewModel.toggleSetDone(bench().instanceId, 0)

        viewModel.finishSession(FinishMode.ALL_SETS)

        assertEquals(listOf(2, 1), repository.stored.single().exercises.map { it.sets.size })
    }

    @Test
    fun `finishing without a template saves the session and updates no template`() {
        viewModel.setExercises(listOf("Squat"))
        val id = viewModel.currentExercises.value.single().instanceId
        viewModel.addSet(id)
        viewModel.updateSetReps(id, 0, "5")
        viewModel.updateSetWeight(id, 0, "100")
        viewModel.toggleSetDone(id, 0)
        assertFalse(viewModel.hasUnfinishedSets())

        viewModel.finishSession(FinishMode.DONE_SETS_ONLY)

        assertEquals(StrengthSet(5, 100.0), repository.stored.single().exercises.single().sets.single())
        assertTrue(repository.templateUpdates.isEmpty())
    }

    @Test
    fun `finishing does nothing when no set would be saved`() {
        viewModel.startSessionFromTemplate(pushDay)

        viewModel.finishSession(FinishMode.DONE_SETS_ONLY)

        assertTrue(repository.stored.isEmpty())
        assertEquals("Push Day", viewModel.activeTemplateName.value)
    }

    @Test
    fun `a failed save surfaces in workoutError and keeps the session`() {
        repository.failNext = IllegalStateException("offline")
        viewModel.startSessionFromTemplate(pushDay)
        viewModel.toggleSetDone(bench().instanceId, 0)

        viewModel.finishSession(FinishMode.DONE_SETS_ONLY)

        assertEquals("offline", viewModel.workoutError.value)
        assertTrue(repository.stored.isEmpty())
        assertEquals("Push Day", viewModel.activeTemplateName.value)
        viewModel.clearWorkoutError()
        assertNull(viewModel.workoutError.value)
    }
}
