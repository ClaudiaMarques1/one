package app.theone.android.workout.templates.frontend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.theone.android.ui.theme.OneTheme
import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.TemplateSet

@Composable
fun TemplateBuilderContent(
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
