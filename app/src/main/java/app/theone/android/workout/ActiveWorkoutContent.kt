package app.theone.android.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.theone.android.ui.theme.OneTheme
import app.theone.android.ui.theme.SwipeDeleteBackground
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutContent(
    templateName: String,
    startTimeMillis: Long?,
    exercises: List<StrengthExercise>,
    onAddSet: (Int, StrengthSet) -> Unit,
    onUpdateSet: (Int, Int, StrengthSet) -> Unit,
    onRemoveSet: (Int, Int) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onDuplicateExercise: (Int) -> Unit,
    onAddExerciseClick: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    val restDurations = remember { mutableStateMapOf<Int, Int>() }
    var restPickerForIndex by remember { mutableStateOf<Int?>(null) }

    var activeRestExercise by remember { mutableStateOf<String?>(null) }
    var activeRestRemaining by remember { mutableStateOf(0) }
    var isRestRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRestRunning) {
        while (isRestRunning) {
            delay(1000)
            val next = activeRestRemaining - 1
            if (next <= 0) {
                activeRestRemaining = 0
                isRestRunning = false
            } else {
                activeRestRemaining = next
            }
        }
    }

    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isRestRunning || activeRestRemaining > 0) {
                    ActiveRestBar(
                        exerciseName = activeRestExercise ?: "",
                        remainingSeconds = activeRestRemaining,
                        onAdjust = { delta -> activeRestRemaining = (activeRestRemaining + delta).coerceAtLeast(0) },
                        onCancel = {
                            isRestRunning = false
                            activeRestRemaining = 0
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(templateName, style = MaterialTheme.typography.headlineSmall)
                    if (startTimeMillis != null) {
                        ElapsedTimeText(startTimeMillis = startTimeMillis)
                    }
                }
            }

            itemsIndexed(exercises) { index, exercise ->
                ExerciseLogCard(
                    exercise = exercise,
                    restSeconds = restDurations[index] ?: 60,
                    onAddSet = { onAddSet(index, StrengthSet()) },
                    onUpdateSet = { setIndex, set -> onUpdateSet(index, setIndex, set) },
                    onRemoveSet = { setIndex -> onRemoveSet(index, setIndex) },
                    onDelete = { onRemoveExercise(index) },
                    onCopy = { onDuplicateExercise(index) },
                    onRestIconClick = { restPickerForIndex = index },
                    onSetDone = {
                        activeRestExercise = exercise.name
                        activeRestRemaining = restDurations[index] ?: 60
                        isRestRunning = true
                    }
                )
            }

            item {
                OutlinedButton(onClick = onAddExerciseClick, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Add exercise")
                }
            }
        }
    }

    restPickerForIndex?.let { index ->
        RestDurationPickerDialog(
            currentSeconds = restDurations[index] ?: 60,
            onDismiss = { restPickerForIndex = null },
            onSelect = { seconds ->
                restDurations[index] = seconds
                restPickerForIndex = null
            }
        )
    }
}

@Composable
private fun ElapsedTimeText(startTimeMillis: Long) {
    var elapsedSeconds by remember(startTimeMillis) {
        mutableStateOf((System.currentTimeMillis() - startTimeMillis) / 1000)
    }
    LaunchedEffect(startTimeMillis) {
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000
            delay(1000)
        }
    }
    Text(formatElapsedTime(elapsedSeconds), style = MaterialTheme.typography.titleMedium)
}

private fun formatElapsedTime(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
}

private val restDurationOptions = listOf(60, 90, 120)

private fun restDurationLabel(seconds: Int): String =
    if (seconds % 60 == 0) "${seconds / 60}:00" else "${seconds}s"

@Composable
private fun ActiveRestBar(
    exerciseName: String,
    remainingSeconds: Int,
    onAdjust: (Int) -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Resting — $exerciseName", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatElapsedTime(remainingSeconds.toLong()), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = { onAdjust(-30) }, modifier = Modifier.weight(1f)) {
                    Text("-30s")
                }
                OutlinedButton(onClick = { onAdjust(30) }, modifier = Modifier.weight(1f)) {
                    Text("+30s")
                }
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
private fun RestDurationPickerDialog(currentSeconds: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rest timer for this exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                restDurationOptions.forEach { seconds ->
                    if (currentSeconds == seconds) {
                        Button(onClick = { onSelect(seconds) }, modifier = Modifier.fillMaxWidth()) {
                            Text(restDurationLabel(seconds))
                        }
                    } else {
                        OutlinedButton(onClick = { onSelect(seconds) }, modifier = Modifier.fillMaxWidth()) {
                            Text(restDurationLabel(seconds))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ExerciseLogCard(
    exercise: StrengthExercise,
    restSeconds: Int,
    onAddSet: () -> Unit,
    onUpdateSet: (Int, StrengthSet) -> Unit,
    onRemoveSet: (Int) -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onRestIconClick: () -> Unit,
    onSetDone: () -> Unit
) {
    SwipeableExerciseCard(onDelete = onDelete, onCopy = onCopy) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = onRestIconClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Timer,
                            contentDescription = "Set rest timer (currently ${restDurationLabel(restSeconds)})",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                exercise.sets.forEachIndexed { setIndex, set ->
                    SetRow(
                        index = setIndex,
                        set = set,
                        onUpdate = { updated -> onUpdateSet(setIndex, updated) },
                        onRemove = { onRemoveSet(setIndex) },
                        onDone = onSetDone
                    )
                }

                OutlinedButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Add set")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableExerciseCard(onDelete: () -> Unit, onCopy: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onCopy()
                    false
                }
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SwipeDeleteBackground, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Delete", color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White)
                    }
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy", color = Color.White)
                    }
                }
                SwipeToDismissBoxValue.Settled -> {}
            }
        },
        content = { content() }
    )
}

@Composable
private fun SetRow(
    index: Int,
    set: StrengthSet,
    onUpdate: (StrengthSet) -> Unit,
    onRemove: () -> Unit,
    onDone: () -> Unit
) {
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
                if (reps != null) {
                    onUpdate(set.copy(reps = reps))
                } else if (text.isEmpty()) {
                    onUpdate(set.copy(reps = 0))
                }
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
                if (weight != null) {
                    onUpdate(set.copy(weightKg = weight))
                } else if (text.isEmpty()) {
                    onUpdate(set.copy(weightKg = 0.0))
                }
            },
            label = { Text("Weight (kg)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onDone) {
            Text("Done")
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Remove set ${index + 1}",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Active workout")
@Composable
private fun ActiveWorkoutContentPreview() {
    OneTheme {
        ActiveWorkoutContent(
            templateName = "Push Day",
            startTimeMillis = System.currentTimeMillis() - 125_000,
            exercises = listOf(
                StrengthExercise("Bench Press", listOf(StrengthSet(8, 60.0))),
                StrengthExercise("Lateral Raise")
            ),
            onAddSet = { _, _ -> },
            onUpdateSet = { _, _, _ -> },
            onRemoveSet = { _, _ -> },
            onRemoveExercise = {},
            onDuplicateExercise = {},
            onAddExerciseClick = {},
            onFinish = {},
            onCancel = {}
        )
    }
}
