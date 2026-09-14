package com.example.one.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.one.exercise.Exercise
import com.example.one.exercise.ExercisePickerDialog
import com.example.one.ui.theme.OneTheme

@Composable
fun WorkoutScreen(
    templateViewModel: WorkoutTemplateViewModel = viewModel(),
    strengthViewModel: StrengthViewModel = viewModel()
) {
    val templates by templateViewModel.templates.collectAsState()
    val sessions by strengthViewModel.sessions.collectAsState()
    val draftName by templateViewModel.draftName.collectAsState()
    val draftDescription by templateViewModel.draftDescription.collectAsState()
    val draftExercises by templateViewModel.draftExercises.collectAsState()
    val editingTemplate by templateViewModel.editingTemplate.collectAsState()
    val activeTemplateName by strengthViewModel.activeTemplateName.collectAsState()
    val currentExercises by strengthViewModel.currentExercises.collectAsState()

    var isBuildingTemplate by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var pendingStartTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }

    when {
        activeTemplateName.isNotEmpty() -> {
            ActiveWorkoutContent(
                templateName = activeTemplateName,
                exercises = currentExercises,
                onAddSet = { index, set -> strengthViewModel.addSet(index, set) },
                onUpdateSet = { exerciseIndex, setIndex, set -> strengthViewModel.updateSet(exerciseIndex, setIndex, set) },
                onRemoveSet = { exerciseIndex, setIndex -> strengthViewModel.removeSet(exerciseIndex, setIndex) },
                onRemoveExercise = { strengthViewModel.removeExercise(it) },
                onDuplicateExercise = { strengthViewModel.duplicateExercise(it) },
                onFinish = { strengthViewModel.saveSession() },
                onCancel = { strengthViewModel.discardSession() }
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

@Composable
private fun WorkoutHomeContent(
    templates: List<WorkoutTemplate>,
    sessions: List<StrengthSession>,
    onCreateTemplateClick: () -> Unit,
    onTemplateClick: (WorkoutTemplate) -> Unit,
    onEditTemplate: (WorkoutTemplate) -> Unit,
    onDeleteTemplate: (String) -> Unit
) {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Workout Templates", style = MaterialTheme.typography.headlineSmall) }

            item {
                Button(onClick = onCreateTemplateClick, modifier = Modifier.fillMaxWidth()) {
                    Text("New template")
                }
            }

            if (templates.isEmpty()) {
                item {
                    Text(
                        "No templates yet — create one to plan a workout you can reuse.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        onClick = { onTemplateClick(template) },
                        onEdit = { onEditTemplate(template) },
                        onDelete = { onDeleteTemplate(template.id) }
                    )
                }
            }

            if (sessions.isNotEmpty()) {
                item { Text("Last workouts", style = MaterialTheme.typography.titleMedium) }
                items(sessions.take(3), key = { it.id }) { session ->
                    SessionCard(session = session)
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: WorkoutTemplate,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(template.name, style = MaterialTheme.typography.titleMedium)
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Template options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            if (template.description.isNotBlank()) {
                Text(template.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TemplateBuilderContent(
    name: String,
    description: String,
    exercises: List<TemplateExercise>,
    isEditing: Boolean,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddExercisesClick: () -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    if (isEditing) "Edit template" else "New template",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Template name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(onClick = onAddExercisesClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Add exercises")
                }
            }

            if (exercises.isNotEmpty()) {
                itemsIndexed(exercises) { index, exercise ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(exercise.name)
                                if (exercise.sets.isNotEmpty()) {
                                    Text(
                                        "Last time: " + exercise.sets.joinToString(", ") { "${it.reps}×${it.weightKg}kg" },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            IconButton(onClick = { onRemoveExercise(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove ${exercise.name}")
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onSave,
                        enabled = name.isNotBlank() && exercises.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEditing) "Save changes" else "Save template")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutContent(
    templateName: String,
    exercises: List<StrengthExercise>,
    onAddSet: (Int, StrengthSet) -> Unit,
    onUpdateSet: (Int, Int, StrengthSet) -> Unit,
    onRemoveSet: (Int, Int) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onDuplicateExercise: (Int) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(templateName, style = MaterialTheme.typography.headlineSmall) }

            itemsIndexed(exercises) { index, exercise ->
                ExerciseLogCard(
                    exercise = exercise,
                    onAddSet = { onAddSet(index, StrengthSet()) },
                    onUpdateSet = { setIndex, set -> onUpdateSet(index, setIndex, set) },
                    onRemoveSet = { setIndex -> onRemoveSet(index, setIndex) },
                    onDelete = { onRemoveExercise(index) },
                    onCopy = { onDuplicateExercise(index) }
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Cancel workout")
                    }
                    Button(
                        onClick = onFinish,
                        enabled = exercises.any { it.sets.isNotEmpty() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Finish workout")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLogCard(
    exercise: StrengthExercise,
    onAddSet: () -> Unit,
    onUpdateSet: (Int, StrengthSet) -> Unit,
    onRemoveSet: (Int) -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                Row {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy exercise")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove exercise")
                    }
                }
            }

            exercise.sets.forEachIndexed { setIndex, set ->
                SetRow(
                    index = setIndex,
                    set = set,
                    onUpdate = { updated -> onUpdateSet(setIndex, updated) },
                    onRemove = { onRemoveSet(setIndex) }
                )
            }

            OutlinedButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth()) {
                Text("+ Add set")
            }
        }
    }
}

@Composable
private fun SetRow(index: Int, set: StrengthSet, onUpdate: (StrengthSet) -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${index + 1}", modifier = Modifier.width(20.dp))
        OutlinedTextField(
            value = if (set.reps == 0) "" else set.reps.toString(),
            onValueChange = { text ->
                val reps = text.toIntOrNull()
                if (reps != null) onUpdate(set.copy(reps = reps)) else if (text.isEmpty()) onUpdate(set.copy(reps = 0))
            },
            label = { Text("Reps") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = if (set.weightKg == 0.0) "" else set.weightKg.toString(),
            onValueChange = { text ->
                val weight = text.replace(',', '.').toDoubleOrNull()
                if (weight != null) onUpdate(set.copy(weightKg = weight)) else if (text.isEmpty()) onUpdate(set.copy(weightKg = 0.0))
            },
            label = { Text("Weight (kg)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Remove set ${index + 1}",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SessionCard(session: StrengthSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(session.date, style = MaterialTheme.typography.titleSmall)
            if (session.templateName.isNotEmpty()) {
                Text(session.templateName, style = MaterialTheme.typography.bodySmall)
            }
            session.exercises.forEach { exercise ->
                Text("${exercise.name}: " + exercise.sets.joinToString(", ") { "${it.reps}×${it.weightKg}kg" })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutHomeContentPreview() {
    OneTheme {
        WorkoutHomeContent(
            templates = listOf(
                WorkoutTemplate(
                    id = "1",
                    name = "Push Day",
                    exercises = listOf(
                        TemplateExercise("Bench Press"),
                        TemplateExercise("Lateral Raise"),
                        TemplateExercise("Tricep Pushdown")
                    )
                )
            ),
            sessions = listOf(
                StrengthSession(
                    id = "1",
                    date = "2026-09-12",
                    exercises = listOf(StrengthExercise("Bench Press", listOf(StrengthSet(8, 60.0))))
                )
            ),
            onCreateTemplateClick = {},
            onTemplateClick = {},
            onEditTemplate = {},
            onDeleteTemplate = {},
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun WorkoutHomeContentEmptyPreview() {
    OneTheme {
        WorkoutHomeContent(
            templates = emptyList(),
            sessions = emptyList(),
            onCreateTemplateClick = {},
            onTemplateClick = {},
            onEditTemplate = {},
            onDeleteTemplate = {},
        )
    }
}

@Preview(showBackground = true, name = "Template builder")
@Composable
private fun TemplateBuilderContentPreview() {
    OneTheme {
        TemplateBuilderContent(
            name = "Push Day",
            description = "Chest, shoulders, triceps",
            exercises = listOf(
                TemplateExercise("Bench Press", listOf(TemplateSet(8, 60.0))),
                TemplateExercise("Lateral Raise")
            ),
            isEditing = false,
            onNameChange = {},
            onDescriptionChange = {},
            onAddExercisesClick = {},
            onRemoveExercise = {},
            onCancel = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, name = "Active workout")
@Composable
private fun ActiveWorkoutContentPreview() {
    OneTheme {
        ActiveWorkoutContent(
            templateName = "Push Day",
            exercises = listOf(
                StrengthExercise("Bench Press", listOf(StrengthSet(8, 60.0))),
                StrengthExercise("Lateral Raise")
            ),
            onAddSet = { _, _ -> },
            onUpdateSet = { _, _, _ -> },
            onRemoveSet = { _, _ -> },
            onRemoveExercise = {},
            onDuplicateExercise = {},
            onFinish = {},
            onCancel = {}
        )
    }
}
