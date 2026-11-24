package app.theone.android.workout.sessions.backend

import androidx.lifecycle.SavedStateHandle
import app.theone.android.testing.MainDispatcherRule
import app.theone.android.workout.templates.backend.FakeWorkoutTemplateRepository
import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.TemplateSet
import app.theone.android.workout.templates.backend.WorkoutTemplate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SessionDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val logged = StrengthSession(
        id = "s1",
        date = "2026-10-08",
        templateName = "Push day",
        exercises = listOf(
            StrengthExercise("Bench press", listOf(StrengthSet(6, 80.0), StrengthSet(5, 80.0))),
            StrengthExercise("Triceps dips", listOf(StrengthSet(10, 0.0)))
        ),
        notes = "Try 82.5 next time"
    )
    private val sessions = FakeStrengthRepository(listOf(logged))
    private val pushDay = WorkoutTemplate(
        id = "t1",
        name = "Push day",
        exercises = listOf(TemplateExercise("Bench press", restSeconds = 120))
    )
    private val templates = FakeWorkoutTemplateRepository(listOf(pushDay))

    private fun TestScope.viewModel(): SessionDetailViewModel {
        val viewModel = SessionDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(SessionDetailViewModel.ARG_SESSION_ID to "s1")),
            repository = sessions,
            templateRepository = templates,
            bodyweightNames = flowOf(setOf("triceps dips"))
        )
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.detail.collect {} }
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.sourceTemplate.collect {} }
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.templates.collect {} }
        return viewModel
    }

    @Test
    fun `detail is the session from the nav argument, with catalog bodyweight and its template`() = runTest {
        val viewModel = viewModel()

        val detail = viewModel.detail.value!!
        assertEquals("s1", detail.session.id)
        assertEquals(3, detail.setCount)
        assertEquals(listOf(false, true), detail.exercises.map { it.bodyweight })
        assertEquals(pushDay, viewModel.sourceTemplate.value)
    }

    @Test
    fun `editing fills the fields from the logged sets and saves changes, removed sets and notes`() = runTest {
        val viewModel = viewModel()
        viewModel.startEditing()
        val bench = viewModel.editDraft.value!!.first()
        assertEquals(ActiveSet(repsText = "6", weightText = "80"), bench.sets[0])
        assertEquals(StrengthSet(6, 80.0), bench.previous(0))
        assertEquals("Try 82.5 next time", viewModel.notesDraft.value)

        viewModel.addSet(bench.instanceId) // copies the last set: 80 x 5
        viewModel.updateSetWeight(bench.instanceId, 0, "82,5")
        viewModel.updateSetReps(bench.instanceId, 1, "") // empty keeps the value from before
        viewModel.removeSet(viewModel.editDraft.value!![1].instanceId, 0) // the dips' only set - exercise goes
        viewModel.updateNotes(" Bench moved well ")
        viewModel.saveEditing()

        val saved = sessions.stored.single()
        assertEquals(
            listOf(StrengthExercise("Bench press", listOf(StrengthSet(6, 82.5), StrengthSet(5, 80.0), StrengthSet(5, 80.0)))),
            saved.exercises
        )
        assertEquals("Bench moved well", saved.notes)
        assertNull(viewModel.editDraft.value)
    }

    @Test
    fun `cancelling an edit leaves the session as it was`() = runTest {
        val viewModel = viewModel()
        viewModel.startEditing()
        viewModel.updateSetReps(viewModel.editDraft.value!!.first().instanceId, 0, "99")
        viewModel.cancelEditing()

        assertNull(viewModel.editDraft.value)
        assertEquals(logged, sessions.stored.single())
    }

    @Test
    fun `delete removes the session and reports back`() = runTest {
        val viewModel = viewModel()
        var deleted = false
        viewModel.deleteSession { deleted = true }

        assertTrue(deleted)
        assertTrue(sessions.stored.isEmpty())
    }

    @Test
    fun `a failed delete surfaces the error and stays`() = runTest {
        val viewModel = viewModel()
        sessions.failNext = IllegalStateException("offline")
        var deleted = false
        viewModel.deleteSession { deleted = true }

        assertFalse(deleted)
        assertEquals("offline", viewModel.sessionError.value)
        assertEquals(1, sessions.stored.size)
    }

    @Test
    fun `save as template copies the exercises and sets under a unique name`() = runTest {
        val viewModel = viewModel()
        var savedName: String? = null
        viewModel.saveAsTemplate { savedName = it }

        assertEquals("Push day (2)", savedName)
        val created = templates.stored.last()
        assertEquals("Push day (2)", created.name)
        assertEquals(
            listOf(
                TemplateExercise("Bench press", listOf(TemplateSet(6, 80.0), TemplateSet(5, 80.0)), restSeconds = 120),
                TemplateExercise("Triceps dips", listOf(TemplateSet(10, 0.0)), restSeconds = 90)
            ),
            created.exercises
        )
    }

    @Test
    fun `uniqueTemplateName counts up past taken names`() {
        assertEquals("Legs", uniqueTemplateName("Legs", setOf("Push")))
        assertEquals("Legs (2)", uniqueTemplateName("Legs", setOf("Legs")))
        assertEquals("Legs (3)", uniqueTemplateName("Legs", setOf("Legs", "Legs (2)")))
    }
}
