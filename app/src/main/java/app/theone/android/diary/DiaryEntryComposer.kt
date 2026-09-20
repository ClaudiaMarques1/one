package app.theone.android.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

private data class BodyRegion(val id: String, val x: Dp, val y: Dp, val width: Dp, val height: Dp, val rounded: Dp)

private val bodyRegions = listOf(
    BodyRegion("shoulders", 20.dp, 50.dp, 140.dp, 20.dp, 10.dp),
    BodyRegion("chest", 50.dp, 68.dp, 80.dp, 45.dp, 12.dp),
    BodyRegion("arms", 8.dp, 68.dp, 28.dp, 110.dp, 14.dp),
    BodyRegion("arms", 144.dp, 68.dp, 28.dp, 110.dp, 14.dp),
    BodyRegion("abs", 55.dp, 111.dp, 70.dp, 55.dp, 10.dp),
    BodyRegion("quads", 48.dp, 168.dp, 38.dp, 80.dp, 14.dp),
    BodyRegion("quads", 94.dp, 168.dp, 38.dp, 80.dp, 14.dp),
    BodyRegion("calves", 50.dp, 250.dp, 32.dp, 70.dp, 12.dp),
    BodyRegion("calves", 96.dp, 250.dp, 32.dp, 70.dp, 12.dp)
)

private val backOnlyParts = listOf("back", "glutes", "hamstrings")

@Composable
fun BodySilhouetteMap(selectedParts: Set<String>, onToggle: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(180.dp, 320.dp)) {
            Box(
                modifier = Modifier
                    .offset(70.dp, 0.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Box(
                modifier = Modifier
                    .offset(72.dp, 38.dp)
                    .size(16.dp, 14.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            bodyRegions.forEach { region ->
                val selected = region.id in selectedParts
                Box(
                    modifier = Modifier
                        .offset(region.x, region.y)
                        .size(region.width, region.height)
                        .clip(RoundedCornerShape(region.rounded))
                        .background(
                            if (selected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onToggle(region.id) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Front view — tap a sore area", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            backOnlyParts.forEach { id ->
                val selected = id in selectedParts
                val label = bodyPartLabels.getValue(id)
                if (selected) {
                    Button(onClick = { onToggle(id) }) { Text(label, style = MaterialTheme.typography.labelSmall) }
                } else {
                    OutlinedButton(onClick = { onToggle(id) }) { Text(label, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

private val moodOptions = listOf(
    "😄" to "Great",
    "🙂" to "Good",
    "😐" to "Okay",
    "😣" to "Sore",
    "😤" to "Frustrated",
    "💪" to "Strong",
    "😴" to "Tired"
)

private val reflectionPrompts = listOf(
    "What held you back today?",
    "What was today's biggest win?",
    "How did your body feel today?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEntryComposerDialog(
    onDismiss: () -> Unit,
    onSave: (mood: String, energyLevel: Int, sleepHours: Int, soreParts: List<String>, reflection: String) -> Unit
) {
    var mood by remember { mutableStateOf(moodOptions.first().first) }
    var energyLevel by remember { mutableStateOf(5f) }
    var sleepHours by remember { mutableStateOf(7f) }
    var soreParts by remember { mutableStateOf(setOf<String>()) }
    var reflection by remember { mutableStateOf("") }
    val prompt = remember { reflectionPrompts.random() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("New entry") },
                    navigationIcon = { TextButton(onClick = onDismiss) { Text("Cancel") } }
                )
            },
            bottomBar = {
                Button(
                    onClick = {
                        onSave(mood, energyLevel.roundToInt(), sleepHours.roundToInt(), soreParts.toList(), reflection)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Save entry")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Column {
                        Text("How do you feel?", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            moodOptions.forEach { (emoji, label) ->
                                val selected = mood == emoji
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .clickable { mood = emoji },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, style = MaterialTheme.typography.headlineSmall)
                                    }
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Energy level: ${energyLevel.roundToInt()}/10", style = MaterialTheme.typography.titleMedium)
                        Slider(value = energyLevel, onValueChange = { energyLevel = it }, valueRange = 1f..10f, steps = 8)
                    }
                }

                item {
                    Column {
                        Text("Sleep: ${sleepHours.roundToInt()}h", style = MaterialTheme.typography.titleMedium)
                        Slider(value = sleepHours, onValueChange = { sleepHours = it }, valueRange = 1f..10f, steps = 8)
                    }
                }

                item {
                    Column {
                        Text("Sore body parts", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        BodySilhouetteMap(
                            selectedParts = soreParts,
                            onToggle = { id -> soreParts = if (id in soreParts) soreParts - id else soreParts + id }
                        )
                    }
                }

                item {
                    Column {
                        Text(prompt, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reflection,
                            onValueChange = { reflection = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
