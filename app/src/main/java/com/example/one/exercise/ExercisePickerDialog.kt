package com.example.one.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerDialog(
    alreadySelectedNames: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onConfirm: (List<Exercise>) -> Unit
) {
    val context = LocalContext.current
    var allExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val selectedIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { ExerciseCatalogLoader.load(context) }
        allExercises = loaded
        selectedCategory = loaded.map { it.bodyPart }.distinct().sorted().firstOrNull()
        selectedIds.addAll(loaded.filter { it.name in alreadySelectedNames }.map { it.id })
        isLoading = false
    }

    val categories = remember(allExercises) { allExercises.map { it.bodyPart }.distinct().sorted() }

    val visibleExercises = remember(query, selectedCategory, allExercises) {
        allExercises
            .filter { selectedCategory == null || it.bodyPart == selectedCategory }
            .filter {
                query.isBlank() ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.target.contains(query, ignoreCase = true)
            }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Add exercises") },
                    navigationIcon = { TextButton(onClick = onDismiss) { Text("Cancel") } }
                )
            },
            bottomBar = {
                Column {
                    Text(
                        EXERCISE_MEDIA_ATTRIBUTION,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Button(
                        onClick = { onConfirm(allExercises.filter { it.id in selectedIds }) },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val count = selectedIds.size
                        Text(if (count == 0) "Select exercises" else "Use $count exercise${if (count == 1) "" else "s"}")
                    }
                }
            }
        ) { padding ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    CategoryRail(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Search") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(visibleExercises, key = { it.id }) { exercise ->
                                ExercisePickerRow(
                                    exercise = exercise,
                                    selected = exercise.id in selectedIds,
                                    onToggle = {
                                        if (exercise.id in selectedIds) selectedIds.remove(exercise.id) else selectedIds.add(exercise.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRail(categories: List<String>, selectedCategory: String?, onSelect: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .width(104.dp)
            .fillMaxHeight()
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(category) }
                    .padding(horizontal = 8.dp, vertical = 14.dp)
            ) {
                Text(
                    category.toDisplayLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

private fun String.toDisplayLabel(): String =
    split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.getDefault()) } }

@Composable
private fun ExercisePickerRow(exercise: Exercise, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = exercise.imageUrl,
            contentDescription = exercise.name,
            modifier = Modifier.size(48.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
            Text("${exercise.target} · ${exercise.equipment}", style = MaterialTheme.typography.bodySmall)
        }
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
    }
}
