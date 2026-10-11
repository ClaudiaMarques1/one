package app.theone.android.workout.frontend

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.theone.android.R
import app.theone.android.ui.formatDisplayDate
import app.theone.android.ui.theme.OneTheme
import app.theone.android.workout.sessions.backend.StrengthExercise
import app.theone.android.workout.sessions.backend.StrengthSession
import app.theone.android.workout.sessions.backend.StrengthSet
import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.WorkoutTemplate

@Composable
fun WorkoutHomeContent(
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
            item { Text(stringResource(R.string.workout_templates_title), style = MaterialTheme.typography.headlineSmall) }

            item {
                Button(onClick = onCreateTemplateClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.workout_new_template))
                }
            }

            if (templates.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.workout_no_templates),
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
                item { Text(stringResource(R.string.workout_last_workouts), style = MaterialTheme.typography.titleMedium) }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(template.name, style = MaterialTheme.typography.titleMedium)
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.workout_template_options))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workout_edit)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workout_delete)) },
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
private fun SessionCard(session: StrengthSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(formatDisplayDate(session.date), style = MaterialTheme.typography.titleSmall)
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
