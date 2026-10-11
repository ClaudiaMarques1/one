package app.theone.android.diary

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import kotlin.random.Random

private val Accent = Color(0xFF8E24AA)
private val AccentInk = Color(0xFF6A1B86)
private val AccentDeepInk = Color(0xFF4A1260)
private val AccentContainer = Color(0xFFF3E3F7)
private val EnergyPartial = Color(0xFFC99AD6)
private val EnergyPartialText = Color(0xFF3D0F50)
private val OnSurface = Color(0xFF1D1B20)
private val SecondaryText = Color(0xFF6B655F)
private val BodyText = Color(0xFF3B3733)
private val Hint = Color(0xFF8A837C)
private val OutlineButton = Color(0xFFCFC6BE)
private val OutlineSegmented = Color(0xFF79747E)
private val Divider = Color(0xFFF0EBE6)
private val IdleTile = Color(0xFFF4F1EE)
private val LevelColors = listOf(Color(0xFFD9B3E4), Color(0xFFAD5BC4), Color(0xFF6A1B86)) // Mild, Sore, Very sore

private val reflectionPrompts = listOf(
    "What was today's biggest win?",
    "What held you back today?",
    "How did your body feel today?"
)

/** 0 (none) -> 1 (Mild) -> 2 (Sore) -> 3 (Very sore) -> 0. */
internal fun cycleSorenessLevel(soreness: Map<String, Int>, partId: String): Map<String, Int> {
    val next = ((soreness[partId] ?: 0) + 1) % 4
    return if (next == 0) soreness - partId else soreness + (partId to next)
}

internal fun clampSleepHours(value: Int): Int = value.coerceIn(0, 14)

internal fun sorenessCount(soreness: Map<String, Int>, ids: List<String>): Int = ids.count { soreness.containsKey(it) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEntryComposerDialog(
    existingEntry: DiaryEntry? = null,
    onDismiss: () -> Unit,
    onSave: (mood: String, energyLevel: Int, sleepHours: Int, soreness: Map<String, Int>, reflection: String) -> Unit
) {
    var mood by rememberSaveable(existingEntry) { mutableStateOf(existingEntry?.mood ?: moodOptions.first().first) }
    var energy by rememberSaveable(existingEntry) { mutableIntStateOf(existingEntry?.energyLevel ?: 5) }
    var sleep by rememberSaveable(existingEntry) { mutableIntStateOf(existingEntry?.sleepHours ?: 7) }
    var side by rememberSaveable(existingEntry) { mutableStateOf(BodySide.FRONT) }
    var soreness by rememberSaveable(existingEntry) { mutableStateOf(existingEntry?.effectiveSoreness ?: emptyMap()) }
    var reflection by rememberSaveable(existingEntry) { mutableStateOf(existingEntry?.reflection ?: "") }
    var promptIndex by rememberSaveable { mutableIntStateOf(Random.nextInt(reflectionPrompts.size)) }

    val subtitleDate = remember(existingEntry) {
        val date = existingEntry?.let { runCatching { DiaryDateUtils.parseDateKey(it.date) }.getOrNull() }
        DiaryDateUtils.fullDateLabel(date ?: LocalDate.now())
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFFFFBFE),
            topBar = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = OnSurface, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(
                                if (existingEntry != null) "Edit entry" else "New entry",
                                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            )
                            Text(subtitleDate, style = TextStyle(fontSize = 13.sp, color = SecondaryText))
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Divider))
                }
            },
            bottomBar = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Divider))
                    Button(
                        onClick = { onSave(mood, energy, sleep, soreness, reflection) },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 16.dp).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text("Save entry", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(top = 20.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                item {
                    MoodSection(mood = mood, onSelect = { mood = it })
                }
                item {
                    EnergySection(energy = energy, onSelect = { energy = it })
                }
                item {
                    SleepSection(sleep = sleep, onChange = { sleep = clampSleepHours(it) })
                }
                item {
                    SoreSection(
                        side = side,
                        onSideChange = { side = it },
                        soreness = soreness,
                        onCycle = { id -> soreness = cycleSorenessLevel(soreness, id) },
                        onClear = { id -> soreness = soreness - id }
                    )
                }
                item {
                    ReflectionSection(
                        prompt = reflectionPrompts[promptIndex],
                        onNextPrompt = { promptIndex = (promptIndex + 1) % reflectionPrompts.size },
                        reflection = reflection,
                        onReflectionChange = { reflection = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface))
        Text(value, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AccentInk))
    }
}

@Composable
private fun MoodSection(mood: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("How do you feel?", moodLabel(mood))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            moodOptions.forEach { (emoji, label) ->
                val selected = mood == emoji
                val background by animateColorAsState(if (selected) AccentContainer else IdleTile, tween(150), label = "moodFill")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(background)
                        .then(if (selected) Modifier.border(2.dp, Accent, RoundedCornerShape(16.dp)) else Modifier)
                        .clickable { onSelect(emoji) }
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, style = TextStyle(fontSize = 26.sp))
                }
            }
        }
    }
}

@Composable
private fun EnergySection(energy: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Energy", "$energy / 10")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            for (n in 1..10) {
                val background = when {
                    n == energy -> Accent
                    n < energy -> EnergyPartial
                    else -> Divider
                }
                val textColor = when {
                    n == energy -> Color.White
                    n < energy -> EnergyPartialText
                    else -> SecondaryText
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(background)
                        .clickable { onSelect(n) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(n.toString(), style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Drained", style = TextStyle(fontSize = 12.sp, color = SecondaryText))
            Text("Full of energy", style = TextStyle(fontSize = 12.sp, color = SecondaryText))
        }
    }
}

@Composable
private fun SleepSection(sleep: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Sleep", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface))
            Text("Last night", style = TextStyle(fontSize = 13.sp, color = SecondaryText))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SleepStepperButton(icon = Icons.Filled.Remove, contentDescription = "Less sleep", onClick = { onChange(sleep - 1) })
            Text(
                "$sleep h",
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(64.dp)
            )
            SleepStepperButton(icon = Icons.Filled.Add, contentDescription = "More sleep", onClick = { onChange(sleep + 1) })
        }
    }
}

@Composable
private fun SleepStepperButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, OutlineButton, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = OnSurface, modifier = Modifier.size(22.dp))
    }
}

private data class SideOption(val side: BodySide, val label: String, val ids: List<String>)

private val sideOptions = listOf(
    SideOption(BodySide.FRONT, "Front", BodyFigureData.frontIds),
    SideOption(BodySide.BACK, "Back", BodyFigureData.backIds)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoreSection(
    side: BodySide,
    onSideChange: (BodySide) -> Unit,
    soreness: Map<String, Int>,
    onCycle: (String) -> Unit,
    onClear: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Anything sore?", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface))
            Text("Tap again for more", style = TextStyle(fontSize = 13.sp, color = SecondaryText))
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            sideOptions.forEachIndexed { index, option ->
                val count = sorenessCount(soreness, option.ids)
                SegmentedButton(
                    selected = side == option.side,
                    onClick = { onSideChange(option.side) },
                    shape = SegmentedButtonDefaults.itemShape(index, sideOptions.size, RoundedCornerShape(20.dp)),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = AccentContainer,
                        activeContentColor = AccentDeepInk,
                        activeBorderColor = OutlineSegmented,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = OnSurface,
                        inactiveBorderColor = OutlineSegmented
                    ),
                    icon = {},
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(option.label, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium))
                            if (count > 0) CountBadge(count)
                        }
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BodyFigure(side = side, levelFor = { soreness[it] ?: 0 }, onTapPart = onCycle, levelColors = LevelColors)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                sorenessLevelLabels.forEachIndexed { index, label ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(14.dp).background(LevelColors[index], RoundedCornerShape(4.dp)))
                        Text(label, style = TextStyle(fontSize = 12.sp, color = BodyText))
                    }
                }
            }
        }

        if (soreness.isEmpty()) {
            Text(
                "Tap anywhere that feels sore. Arms, legs and shoulders count both sides.",
                style = TextStyle(fontSize = 13.sp, color = Hint)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bodyPartOrder.filter { soreness.containsKey(it) }.forEach { id ->
                    SoreRow(
                        label = bodyPartLabels[id] ?: id,
                        levelIndex = (soreness[id] ?: 1) - 1,
                        onCycle = { onCycle(id) },
                        onRemove = { onClear(id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Box(
        modifier = Modifier
            .height(20.dp)
            .background(Accent, RoundedCornerShape(10.dp))
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(count.toString(), style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
    }
}

@Composable
private fun SoreRow(label: String, levelIndex: Int, onCycle: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFAF7F4)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onCycle)
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).background(LevelColors[levelIndex.coerceIn(0, 2)], RoundedCornerShape(3.dp)))
            Text(
                label,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OnSurface),
                modifier = Modifier.weight(1f)
            )
            Text(sorenessLevelLabels[levelIndex.coerceIn(0, 2)], style = TextStyle(fontSize = 13.sp, color = SecondaryText))
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Remove", tint = SecondaryText, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ReflectionSection(prompt: String, onNextPrompt: () -> Unit, reflection: String, onReflectionChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                prompt,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNextPrompt, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = "Next prompt", tint = SecondaryText, modifier = Modifier.size(20.dp))
            }
        }
        OutlinedTextField(
            value = reflection,
            onValueChange = onReflectionChange,
            placeholder = { Text("Write a few words…") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = OutlineButton),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, color = OnSurface)
        )
    }
}
