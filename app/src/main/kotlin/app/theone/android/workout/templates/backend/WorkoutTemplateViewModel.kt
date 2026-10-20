package app.theone.android.workout.templates.backend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutTemplateViewModel(
    private val repository: WorkoutTemplateRepository = FirestoreWorkoutTemplateRepository()
) : ViewModel() {

    private val _templateError = MutableStateFlow<String?>(null)
    val templateError: StateFlow<String?> = _templateError

    val templates: StateFlow<List<WorkoutTemplate>> = repository.observeTemplates()
        .catch { e -> _templateError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearTemplateError() {
        _templateError.value = null
    }

    private val _draftName = MutableStateFlow("")
    val draftName: StateFlow<String> = _draftName

    private val _draftDescription = MutableStateFlow("")
    val draftDescription: StateFlow<String> = _draftDescription

    private val _draftExercises = MutableStateFlow<List<TemplateExercise>>(emptyList())
    val draftExercises: StateFlow<List<TemplateExercise>> = _draftExercises

    private val _editingTemplate = MutableStateFlow<WorkoutTemplate?>(null)
    val editingTemplate: StateFlow<WorkoutTemplate?> = _editingTemplate

    fun updateDraftName(name: String) {
        _draftName.value = name
    }

    fun updateDraftDescription(description: String) {
        _draftDescription.value = description
    }

    fun setDraftExercises(names: List<String>) {
        val existingByName = _draftExercises.value.associateBy { it.name }
        _draftExercises.value = names.map { name -> existingByName[name] ?: TemplateExercise(name = name) }
    }

    fun removeDraftExercise(index: Int) {
        _draftExercises.value = _draftExercises.value.toMutableList().apply { removeAt(index) }
    }

    fun startEditingTemplate(template: WorkoutTemplate) {
        _editingTemplate.value = template
        _draftName.value = template.name
        _draftDescription.value = template.description
        _draftExercises.value = template.exercises
    }

    fun clearDraft() {
        _draftName.value = ""
        _draftDescription.value = ""
        _draftExercises.value = emptyList()
        _editingTemplate.value = null
    }

    fun saveDraftTemplate() {
        val name = _draftName.value
        val description = _draftDescription.value
        val exercises = _draftExercises.value
        if (name.isBlank() || exercises.isEmpty()) return
        val editing = _editingTemplate.value
        viewModelScope.launch {
            runCatching {
                if (editing != null) {
                    repository.updateTemplate(editing.copy(name = name, description = description, exercises = exercises))
                } else {
                    repository.addTemplate(WorkoutTemplate(name = name, description = description, exercises = exercises))
                }
            }.onFailure { _templateError.value = it.message }
                .onSuccess { clearDraft() }
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteTemplate(id) }
                .onFailure { _templateError.value = it.message }
        }
    }
}
