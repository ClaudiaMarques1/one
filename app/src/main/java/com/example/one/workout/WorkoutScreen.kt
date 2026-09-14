package com.example.one.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
fun WorkoutScreen(viewModel: StrengthViewModel = viewModel()) {
    val sessions by viewModel.sessions.collectAsState()
    val currentExercises by viewModel.currentExercises.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    WorkoutScreenContent(
        sessions = sessions,
        currentExercises = currentExercises,
        onAddExercisesClick = { showPicker = true },
        onAddSet = { index, set -> viewModel.addSet(index, set) },
        onRemoveExercise = { viewModel.removeExercise(it) },
        onSaveSession = { viewModel.saveSession() },
        onDeleteSession = { viewModel.deleteSession(it) }
    )

    if (showPicker) {
        ExercisePickerDialog(
            onDismiss = { showPicker = false },
            onConfirm = { selected: List<Exercise> ->
                viewModel.addExercises(selected.map { it.name })
                showPicker = false
            }
        )
    }
}

@Composable
private fun WorkoutScreenContent(
    sessions: List<StrengthSession>,
    currentExercises: List<StrengthExercise>,
    onAddExercisesClick: () -> Unit,
    onAddSet: (Int, StrengthSet) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onSaveSession: () -> Unit,
    onDeleteSession: (String) -> Unit
) {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Strength Training", style = MaterialTheme.typography.headlineSmall) }

            item {
                Button(onClick = onAddExercisesClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Add exercises")
                }
            }

            if (currentExercises.isNotEmpty()) {
                item { Text("This session", style = MaterialTheme.typography.titleMedium) }
                itemsIndexed(currentExercises) { index, exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onAddSet = { set -> onAddSet(index, set) },
                        onDelete = { onRemoveExercise(index) }
                    )
                }
                item {
                    Button(onClick = onSaveSession, modifier = Modifier.fillMaxWidth()) {
                        Text("Save workout")
                    }
                }
            }

            if (sessions.isNotEmpty()) {
                item { Text("History", style = MaterialTheme.typography.titleMedium) }
                items(sessions, key = { it.id }) { session ->
                    SessionCard(session = session, onDelete = { onDeleteSession(session.id) })
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: StrengthExercise, onAddSet: (StrengthSet) -> Unit, onDelete: () -> Unit) {
    var repsInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove exercise")
                }
            }

            if (exercise.sets.isNotEmpty()) {
                Text(exercise.sets.joinToString(", ") { "${it.reps}×${it.weightKg}kg" })
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    label = { Text("Reps") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    val reps = repsInput.toIntOrNull()
                    val weight = weightInput.toDoubleOrNull()
                    if (reps != null && weight != null) {
                        onAddSet(StrengthSet(reps, weight))
                        repsInput = ""
                        weightInput = ""
                    }
                }) {
                    Text("Add set")
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: StrengthSession, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(session.date, style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete session")
                }
            }
            session.exercises.forEach { exercise ->
                Text("${exercise.name}: " + exercise.sets.joinToString(", ") { "${it.reps}×${it.weightKg}kg" })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutScreenPreview() {
    OneTheme {
        WorkoutScreenContent(
            sessions = listOf(
                StrengthSession(
                    id = "1",
                    date = "2026-09-12",
                    exercises = listOf(
                        StrengthExercise("Bench Press", listOf(StrengthSet(8, 60.0), StrengthSet(8, 60.0)))
                    )
                )
            ),
            currentExercises = listOf(
                StrengthExercise("Squat", listOf(StrengthSet(5, 80.0))),
                StrengthExercise("Lateral Raise")
            ),
            onAddExercisesClick = {},
            onAddSet = { _, _ -> },
            onRemoveExercise = {},
            onSaveSession = {},
            onDeleteSession = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun WorkoutScreenEmptyPreview() {
    OneTheme {
        WorkoutScreenContent(
            sessions = emptyList(),
            currentExercises = emptyList(),
            onAddExercisesClick = {},
            onAddSet = { _, _ -> },
            onRemoveExercise = {},
            onSaveSession = {},
            onDeleteSession = {}
        )
    }
}
