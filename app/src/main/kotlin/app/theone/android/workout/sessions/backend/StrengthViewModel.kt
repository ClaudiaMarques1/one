package app.theone.android.workout.sessions.backend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.theone.android.workout.templates.backend.WorkoutTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class StrengthViewModel(
    private val repository: StrengthRepository = FirestoreStrengthRepository(),
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _workoutError = MutableStateFlow<String?>(null)
    val workoutError: StateFlow<String?> = _workoutError

    val sessions: StateFlow<List<StrengthSession>> = repository.observeSessions()
        .catch { e -> _workoutError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearWorkoutError() {
        _workoutError.value = null
    }

    // Backed by SavedStateHandle (not plain MutableStateFlow) so an in-progress session survives
    // process death - see ActiveWorkout.kt's Serializable models.
    val currentExercises: StateFlow<List<ActiveExercise>> =
        savedStateHandle.getStateFlow(KEY_CURRENT_EXERCISES, emptyList())

    /** The template Finish writes back to; "" for a workout without one. */
    val activeTemplateId: StateFlow<String> =
        savedStateHandle.getStateFlow(KEY_ACTIVE_TEMPLATE_ID, "")
    val activeTemplateName: StateFlow<String> =
        savedStateHandle.getStateFlow(KEY_ACTIVE_TEMPLATE_NAME, "")

    val sessionStartTimeMillis: StateFlow<Long?> =
        savedStateHandle.getStateFlow(KEY_SESSION_START_TIME, null)

    /** The running rest timer, or null. Wall-clock based, so it keeps counting across rotation. */
    val restState: StateFlow<RestState?> = savedStateHandle.getStateFlow(KEY_REST, null)

    /** The workout's notes (#48), saved with the session on Finish. */
    val notes: StateFlow<String> = savedStateHandle.getStateFlow(KEY_NOTES, "")

    fun startSessionFromTemplate(template: WorkoutTemplate) {
        startSession(template.id, template.name, template.toActiveExercises())
    }

    /**
     * Session detail's Repeat (#48): the same exercises, with this session's sets as Previous. With the
     * [template] it was logged from (same name), Finish updates that template and its rest times carry over.
     */
    fun repeatSession(session: StrengthSession, template: WorkoutTemplate?) {
        startSession(template?.id.orEmpty(), session.templateName, session.toActiveExercises(template))
    }

    private fun startSession(templateId: String, templateName: String, exercises: List<ActiveExercise>) {
        savedStateHandle[KEY_ACTIVE_TEMPLATE_ID] = templateId
        savedStateHandle[KEY_ACTIVE_TEMPLATE_NAME] = templateName.ifBlank { "Workout" }
        savedStateHandle[KEY_SESSION_START_TIME] = clock()
        savedStateHandle[KEY_CURRENT_EXERCISES] = exercises
        savedStateHandle[KEY_REST] = null
        savedStateHandle[KEY_NOTES] = ""
    }

    fun updateNotes(text: String) {
        savedStateHandle[KEY_NOTES] = text
    }

    fun discardSession() {
        clearSession()
    }

    /**
     * The exercise picker's result: exercises still selected stay (with their sets, in place,
     * duplicates included), newly picked names are appended, deselected ones are removed.
     */
    fun setExercises(names: List<String>) {
        val kept = currentExercises.value.filter { it.name in names }
        val keptNames = kept.map { it.name }.toSet()
        updateExercises { kept + names.filterNot { it in keptNames }.map { ActiveExercise(name = it) } }
    }

    fun updateSetReps(instanceId: String, setIndex: Int, text: String) {
        updateSet(instanceId, setIndex) { it.copy(repsText = text) }
    }

    fun updateSetWeight(instanceId: String, setIndex: Int, text: String) {
        updateSet(instanceId, setIndex) { it.copy(weightText = text) }
    }

    /** Ticking a set; marking it done starts that exercise's rest (unless Off), replacing any running rest. */
    fun toggleSetDone(instanceId: String, setIndex: Int) {
        val exercise = currentExercises.value.find { it.instanceId == instanceId } ?: return
        val toggled = exercise.toggleSetDone(setIndex)
        val nowDone = toggled.sets.getOrNull(setIndex)?.done == true
        // Rest first, so the exercise waits for it to end before moving up (#64).
        if (nowDone && exercise.restSeconds > 0) {
            savedStateHandle[KEY_REST] = RestState(
                exerciseName = exercise.name,
                totalSeconds = exercise.restSeconds,
                endsAtMillis = clock() + exercise.restSeconds * 1000L,
                instanceId = instanceId
            )
        }
        updateExercise(instanceId) { toggled }
    }

    fun addSet(instanceId: String) {
        updateExercise(instanceId) { it.withAddedSet() }
    }

    fun removeSet(instanceId: String, setIndex: Int) {
        updateExercise(instanceId) { exercise ->
            exercise.copy(sets = exercise.sets.filterIndexed { index, _ -> index != setIndex })
        }
    }

    fun removeExercise(instanceId: String) {
        updateExercises { list -> list.filterNot { it.instanceId == instanceId } }
    }

    /** Inserts a copy right below, with no sets, the same rest time and the same previous values. */
    fun duplicateExercise(instanceId: String) {
        updateExercises { list ->
            val index = list.indexOfFirst { it.instanceId == instanceId }
            if (index < 0) return@updateExercises list
            val original = list[index]
            list.toMutableList().apply {
                add(
                    index + 1,
                    ActiveExercise(
                        name = original.name,
                        previousSets = original.previousSets,
                        restSeconds = original.restSeconds,
                    ),
                )
            }
        }
    }

    fun setRestSeconds(instanceId: String, seconds: Int) {
        updateExercise(instanceId) { it.copy(restSeconds = seconds) }
    }

    /** The rest bar's -30 / +30. */
    fun adjustRest(deltaSeconds: Int) {
        savedStateHandle[KEY_REST] = restState.value?.adjusted(deltaSeconds, clock())
        updateExercises { it } // -30 can end the rest
    }

    /** Skip, or the timer reaching 0 - the resting exercise now moves up (#64). */
    fun endRest() {
        savedStateHandle[KEY_REST] = null
        updateExercises { it }
    }

    /** Finish needs a choice from the user when some sets aren't ticked. */
    fun hasUnfinishedSets(): Boolean = currentExercises.value.hasUnfinishedSets()

    /** Logs only the ticked sets; [templateUpdate] decides what goes back to the template (#48). */
    fun finishSession(templateUpdate: TemplateUpdate = TemplateUpdate.KEEP_FULL) {
        val exercises = currentExercises.value
        val sessionExercises = exercises.toSessionExercises(FinishMode.DONE_SETS_ONLY)
        if (sessionExercises.isEmpty()) return
        val templateId = activeTemplateId.value
        val session = StrengthSession(
            date = LocalDate.now().toString(),
            templateName = activeTemplateName.value,
            exercises = sessionExercises,
            createdAt = clock(),
            startedAt = sessionStartTimeMillis.value,
            notes = notes.value.trim()
        )
        viewModelScope.launch {
            runCatching {
                repository.addSessionAndUpdateTemplate(
                    session = session,
                    templateId = templateId.ifEmpty { null },
                    templateExercises = exercises.toTemplateExercises(templateUpdate).takeIf { templateId.isNotEmpty() }
                )
            }.onFailure { _workoutError.value = it.message }
                .onSuccess { clearSession() }
        }
    }

    private fun clearSession() {
        savedStateHandle[KEY_ACTIVE_TEMPLATE_ID] = ""
        savedStateHandle[KEY_ACTIVE_TEMPLATE_NAME] = ""
        savedStateHandle[KEY_SESSION_START_TIME] = null
        savedStateHandle[KEY_CURRENT_EXERCISES] = emptyList<ActiveExercise>()
        savedStateHandle[KEY_REST] = null
        savedStateHandle[KEY_NOTES] = ""
    }

    private fun updateSet(instanceId: String, setIndex: Int, change: (ActiveSet) -> ActiveSet) {
        updateExercise(instanceId) { exercise ->
            val set = exercise.sets.getOrNull(setIndex) ?: return@updateExercise exercise
            exercise.copy(sets = exercise.sets.toMutableList().also { it[setIndex] = change(set) })
        }
    }

    private fun updateExercise(instanceId: String, change: (ActiveExercise) -> ActiveExercise) {
        updateExercises { list -> list.map { if (it.instanceId == instanceId) change(it) else it } }
    }

    // Every change goes through here, so the done order (#64) always matches the ticks and the rest.
    private fun updateExercises(change: (List<ActiveExercise>) -> List<ActiveExercise>) {
        savedStateHandle[KEY_CURRENT_EXERCISES] = change(currentExercises.value).withDoneOrder(restState.value?.instanceId)
    }

    private companion object {
        const val KEY_CURRENT_EXERCISES = "currentExercises"
        const val KEY_ACTIVE_TEMPLATE_ID = "activeTemplateId"
        const val KEY_ACTIVE_TEMPLATE_NAME = "activeTemplateName"
        const val KEY_SESSION_START_TIME = "sessionStartTimeMillis"
        const val KEY_REST = "restState"
        const val KEY_NOTES = "notes"
    }
}
