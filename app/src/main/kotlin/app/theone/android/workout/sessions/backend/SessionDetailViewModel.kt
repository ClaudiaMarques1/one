package app.theone.android.workout.sessions.backend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.theone.android.workout.templates.backend.DEFAULT_REST_SECONDS
import app.theone.android.workout.templates.backend.FirestoreWorkoutTemplateRepository
import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.TemplateSet
import app.theone.android.workout.templates.backend.WorkoutTemplate
import app.theone.android.workout.templates.backend.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Session detail (3a, #48) for the session in the nav argument [ARG_SESSION_ID]: the detail itself, the
 * Edit sets draft (in [savedStateHandle], so it survives rotation), Delete and Save as template.
 * [bodyweightNames] are the exercise catalog's bodyweight exercises, lower-cased (loaded by the screen).
 */
class SessionDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: StrengthRepository = FirestoreStrengthRepository(),
    private val templateRepository: WorkoutTemplateRepository = FirestoreWorkoutTemplateRepository(),
    bodyweightNames: Flow<Set<String>> = flowOf(emptySet())
) : ViewModel() {
    private val sessionId: String = savedStateHandle.get<String>(ARG_SESSION_ID).orEmpty()

    private val _sessionError = MutableStateFlow<String?>(null)
    val sessionError: StateFlow<String?> = _sessionError

    fun clearSessionError() {
        _sessionError.value = null
    }

    private val sessions = repository.observeSessions()
        .catch { e -> _sessionError.value = e.message }

    val detail: StateFlow<SessionDetail?> = combine(sessions, bodyweightNames) { list, bodyweight ->
        buildSessionDetail(sessionId, list, bodyweight)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val templates: StateFlow<List<WorkoutTemplate>> = templateRepository.observeTemplates()
        .catch { e -> _sessionError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The template this session was logged from (same name), for Repeat. */
    val sourceTemplate: StateFlow<WorkoutTemplate?> = combine(detail, templates) { detail, templates ->
        detail?.let { d -> templates.firstOrNull { it.name == d.session.templateName } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Edit sets ---

    /** The exercises being edited, or null when not editing. Previous = the values before editing. */
    val editDraft: StateFlow<List<ActiveExercise>?> = savedStateHandle.getStateFlow(KEY_EDIT_DRAFT, null)
    val notesDraft: StateFlow<String> = savedStateHandle.getStateFlow(KEY_NOTES_DRAFT, "")
    val isEditing: StateFlow<Boolean> = editDraft.map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun startEditing() {
        val session = detail.value?.session ?: return
        savedStateHandle[KEY_EDIT_DRAFT] = session.toEditDraft()
        savedStateHandle[KEY_NOTES_DRAFT] = session.notes
    }

    fun cancelEditing() {
        savedStateHandle[KEY_EDIT_DRAFT] = null
        savedStateHandle[KEY_NOTES_DRAFT] = ""
    }

    fun updateSetReps(instanceId: String, setIndex: Int, text: String) = updateSet(instanceId, setIndex) { it.copy(repsText = text) }

    fun updateSetWeight(instanceId: String, setIndex: Int, text: String) = updateSet(instanceId, setIndex) { it.copy(weightText = text) }

    fun addSet(instanceId: String) = updateExercise(instanceId) { it.withAddedSet() }

    fun removeSet(instanceId: String, setIndex: Int) = updateExercise(instanceId) { exercise ->
        exercise.copy(sets = exercise.sets.filterIndexed { index, _ -> index != setIndex })
    }

    fun updateNotes(text: String) {
        savedStateHandle[KEY_NOTES_DRAFT] = text
    }

    /** Saves the edited sets (an empty field keeps the value from before) and notes; exercises left with no sets go. */
    fun saveEditing() {
        val session = detail.value?.session ?: return
        val draft = editDraft.value ?: return
        val edited = session.copy(exercises = draft.toSessionExercises(FinishMode.ALL_SETS), notes = notesDraft.value.trim())
        viewModelScope.launch {
            runCatching { repository.updateSession(edited) }
                .onFailure { _sessionError.value = it.message }
                .onSuccess { cancelEditing() }
        }
    }

    // --- Menu ---

    fun deleteSession(onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.deleteSession(sessionId) }
                .onFailure { _sessionError.value = it.message }
                .onSuccess { onDeleted() }
        }
    }

    /** A new template with this session's exercises and sets, named after it (made unique). */
    fun saveAsTemplate(onSaved: (name: String) -> Unit) {
        val session = detail.value?.session ?: return
        val source = sourceTemplate.value
        val name = uniqueTemplateName(session.templateName.ifBlank { "Workout" }, templates.value.map { it.name }.toSet())
        val template = WorkoutTemplate(
            name = name,
            exercises = session.exercises.map { exercise ->
                TemplateExercise(
                    name = exercise.name,
                    sets = exercise.sets.map { TemplateSet(it.reps, it.weightKg) },
                    restSeconds = source?.exercises?.firstOrNull { it.name == exercise.name }?.restSeconds ?: DEFAULT_REST_SECONDS
                )
            }
        )
        viewModelScope.launch {
            runCatching { templateRepository.addTemplate(template) }
                .onFailure { _sessionError.value = it.message }
                .onSuccess { onSaved(name) }
        }
    }

    private fun updateSet(instanceId: String, setIndex: Int, change: (ActiveSet) -> ActiveSet) = updateExercise(instanceId) { exercise ->
        val set = exercise.sets.getOrNull(setIndex) ?: return@updateExercise exercise
        exercise.copy(sets = exercise.sets.toMutableList().also { it[setIndex] = change(set) })
    }

    private fun updateExercise(instanceId: String, change: (ActiveExercise) -> ActiveExercise) {
        val draft = editDraft.value ?: return
        savedStateHandle[KEY_EDIT_DRAFT] = draft.map { if (it.instanceId == instanceId) change(it) else it }
    }

    companion object {
        /** The nav argument (`workout/session/{sessionId}`), read from the entry's SavedStateHandle. */
        const val ARG_SESSION_ID = "sessionId"
        private const val KEY_EDIT_DRAFT = "editDraft"
        private const val KEY_NOTES_DRAFT = "notesDraft"
    }
}

/** Edit sets: each logged set as filled-in fields, with the logged values as Previous. */
fun StrengthSession.toEditDraft(newId: () -> String = { UUID.randomUUID().toString() }): List<ActiveExercise> =
    exercises.map { exercise ->
        ActiveExercise(
            name = exercise.name,
            // Not "done": no tick column in Edit sets, and plain field colours read as editable.
            sets = exercise.sets.map { ActiveSet(repsText = it.reps.toString(), weightText = formatWeightInput(it.weightKg)) },
            previousSets = exercise.sets,
            instanceId = newId()
        )
    }

/** [base], or "base (2)", "base (3)"... when a template already has that name. */
fun uniqueTemplateName(base: String, existing: Set<String>): String {
    if (base !in existing) return base
    var n = 2
    while ("$base ($n)" in existing) n++
    return "$base ($n)"
}
