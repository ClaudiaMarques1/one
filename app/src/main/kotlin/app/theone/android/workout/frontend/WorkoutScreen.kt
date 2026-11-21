package app.theone.android.workout.frontend

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.theone.android.exercise.backend.Exercise
import app.theone.android.exercise.frontend.ExercisePickerDialog
import app.theone.android.ui.ErrorBanner
import app.theone.android.workout.sessions.backend.StrengthViewModel
import app.theone.android.workout.sessions.frontend.ActiveWorkoutContent
import app.theone.android.workout.templates.backend.WorkoutTemplate
import app.theone.android.workout.templates.backend.WorkoutTemplateViewModel
import app.theone.android.workout.templates.frontend.TemplateBuilderContent

// Split across WorkoutHomeContent.kt / templates/frontend/TemplateBuilderContent.kt /
// sessions/frontend/ActiveWorkoutContent.kt
// (see .claude/rules/architecture.md) - this file is just the entry point + mode dispatch.
@Composable
fun WorkoutScreen(
    templateViewModel: WorkoutTemplateViewModel = viewModel(),
    strengthViewModel: StrengthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { StrengthViewModel(savedStateHandle = createSavedStateHandle()) }
        }
    )
) {
    val templates by templateViewModel.templates.collectAsState()
    val sessions by strengthViewModel.sessions.collectAsState()
    val draftName by templateViewModel.draftName.collectAsState()
    val draftDescription by templateViewModel.draftDescription.collectAsState()
    val draftExercises by templateViewModel.draftExercises.collectAsState()
    val editingTemplate by templateViewModel.editingTemplate.collectAsState()
    val activeTemplateName by strengthViewModel.activeTemplateName.collectAsState()
    val sessionStartTimeMillis by strengthViewModel.sessionStartTimeMillis.collectAsState()
    val currentExercises by strengthViewModel.currentExercises.collectAsState()
    val restState by strengthViewModel.restState.collectAsState()
    val workoutError by strengthViewModel.workoutError.collectAsState()
    val templateError by templateViewModel.templateError.collectAsState()

    var isBuildingTemplate by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var showActiveWorkoutPicker by remember { mutableStateOf(false) }
    var pendingStartTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            activeTemplateName.isNotEmpty() -> {
                ActiveWorkoutContent(
                    templateName = activeTemplateName,
                    startTimeMillis = sessionStartTimeMillis,
                    exercises = currentExercises,
                    restState = restState,
                    onRepsChange = strengthViewModel::updateSetReps,
                    onWeightChange = strengthViewModel::updateSetWeight,
                    onToggleSetDone = strengthViewModel::toggleSetDone,
                    onAddSet = strengthViewModel::addSet,
                    onRemoveSet = strengthViewModel::removeSet,
                    onDuplicateExercise = strengthViewModel::duplicateExercise,
                    onRemoveExercise = strengthViewModel::removeExercise,
                    onSetRestSeconds = strengthViewModel::setRestSeconds,
                    onAdjustRest = strengthViewModel::adjustRest,
                    onEndRest = strengthViewModel::endRest,
                    onAddExerciseClick = { showActiveWorkoutPicker = true },
                    onFinish = strengthViewModel::finishSession,
                    onDiscard = strengthViewModel::discardSession
                )
            }
            isBuildingTemplate -> {
                TemplateBuilderContent(
                    name = draftName,
                    description = draftDescription,
                    exercises = draftExercises,
                    isEditing = editingTemplate != null,
                    onNameChange = { templateViewModel.updateDraftName(it) },
                    onDescriptionChange = { templateViewModel.updateDraftDescription(it) },
                    onAddExercisesClick = { showPicker = true },
                    onRemoveExercise = { templateViewModel.removeDraftExercise(it) },
                    onCancel = {
                        templateViewModel.clearDraft()
                        isBuildingTemplate = false
                    },
                    onSave = {
                        templateViewModel.saveDraftTemplate()
                        isBuildingTemplate = false
                    }
                )
            }
            else -> {
                WorkoutHomeContent(
                    templates = templates,
                    sessions = sessions,
                    onCreateTemplateClick = { isBuildingTemplate = true },
                    onTemplateClick = { pendingStartTemplate = it },
                    onEditTemplate = {
                        templateViewModel.startEditingTemplate(it)
                        isBuildingTemplate = true
                    },
                    onDeleteTemplate = { templateViewModel.deleteTemplate(it) }
                )
            }
        }

        ErrorBanner(
            message = workoutError ?: templateError,
            onDismiss = {
                strengthViewModel.clearWorkoutError()
                templateViewModel.clearTemplateError()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showPicker) {
        ExercisePickerDialog(
            alreadySelectedNames = draftExercises.map { it.name }.toSet(),
            onDismiss = { showPicker = false },
            onConfirm = { selected: List<Exercise> ->
                templateViewModel.setDraftExercises(selected.map { it.name })
                showPicker = false
            }
        )
    }

    if (showActiveWorkoutPicker) {
        ExercisePickerDialog(
            alreadySelectedNames = currentExercises.map { it.name }.toSet(),
            onDismiss = { showActiveWorkoutPicker = false },
            onConfirm = { selected: List<Exercise> ->
                strengthViewModel.setExercises(selected.map { it.name })
                showActiveWorkoutPicker = false
            }
        )
    }

    pendingStartTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingStartTemplate = null },
            title = { Text("Start workout?") },
            text = {
                Column {
                    template.exercises.forEachIndexed { index, exercise ->
                        Text("${index + 1}. ${exercise.name}")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Would you like to start your workout: ${template.name}?")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    strengthViewModel.startSessionFromTemplate(template)
                    pendingStartTemplate = null
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStartTemplate = null }) {
                    Text("No")
                }
            }
        )
    }
}
