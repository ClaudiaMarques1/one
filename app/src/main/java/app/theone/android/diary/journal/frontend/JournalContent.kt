package app.theone.android.diary.journal.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.diary.journal.backend.DiaryDateKeys
import app.theone.android.diary.journal.backend.DiaryEntry
import app.theone.android.diary.journal.backend.effectiveSoreness
import app.theone.android.ui.EditDeleteMenu
import app.theone.android.ui.theme.OneTheme

private val Accent = Color(0xFF8E24AA)
private val AccentInk = Color(0xFF6A1B86)
private val AccentSoft = Color(0xFFF6EEF8)
private val OnSurface = Color(0xFF1D1B20)
private val SecondaryText = Color(0xFF6B655F)
private val CardBorder = Color(0xFFEAE4DE)
private val ChipFill = Color(0xFFF4F1EE)
private val BodyText = Color(0xFF3B3733)
private val TodayHelper = Color(0xFF5E5560)

@Composable
internal fun JournalContent(
    entries: List<DiaryEntry>,
    onWriteTodayClick: () -> Unit,
    onEditEntry: (DiaryEntry) -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit
) {
    val todayKey = remember { DiaryDateKeys.todayKey() }
    val todayEntry = entries.firstOrNull { it.date == todayKey }
    val grouped = remember(entries, todayKey) {
        entries.filter { it.date != todayKey }
            .groupBy { DiaryDateLabels.groupLabel(it.date, todayKey) }
            .toList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (todayEntry == null) {
                TodayPromptCard(onWriteTodayClick)
            } else {
                EntryCard(entry = todayEntry, onEdit = { onEditEntry(todayEntry) }, onDelete = { onDeleteEntry(todayEntry) })
            }
        }
        grouped.forEach { (label, groupEntries) ->
            item {
                Text(
                    label,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SecondaryText),
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)
                )
            }
            items(groupEntries, key = { it.id }) { entry ->
                EntryCard(entry = entry, onEdit = { onEditEntry(entry) }, onDelete = { onDeleteEntry(entry) })
            }
        }
    }
}

@Composable
private fun TodayPromptCard(onWriteTodayClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AccentSoft)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                DiaryDateLabels.fullDateLabel(DiaryDateKeys.todayKey()),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AccentInk)
            )
            Text(
                "How did today go?",
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "Takes about a minute. Mood, energy, sleep and anything sore.",
                style = TextStyle(fontSize = 14.sp, color = TodayHelper),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Accent)
                .clickable(onClick = onWriteTodayClick)
                .padding(start = 16.dp, end = 20.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.EditNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text("Write today's entry", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryCard(entry: DiaryEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val soreness = entry.effectiveSoreness

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(start = 14.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(44.dp).background(AccentSoft, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.mood, style = TextStyle(fontSize = 24.sp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    DiaryDateLabels.fullDateLabel(entry.date),
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    moodLabel(entry.mood),
                    style = TextStyle(fontSize = 13.sp, color = SecondaryText),
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color(0xFF49454F), modifier = Modifier.size(22.dp))
                }
                EditDeleteMenu(expanded = menuOpen, onDismiss = { menuOpen = false }, onEdit = onEdit, onDelete = onDelete)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(end = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                Text("Energy ${entry.energyLevel}/10", style = TextStyle(fontSize = 13.sp, color = BodyText))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Bedtime, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                Text("Sleep ${entry.sleepHours} h", style = TextStyle(fontSize = 13.sp, color = BodyText))
            }
        }

        if (soreness.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bodyPartOrder.filter { soreness.containsKey(it) }.forEach { id ->
                    val levelIndex = (soreness[id] ?: 1) - 1
                    val label = (bodyPartLabels[id] ?: id) + " · " + sorenessLevelLabels[levelIndex.coerceIn(0, 2)]
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ChipFill)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(label, style = TextStyle(fontSize = 12.sp, color = BodyText))
                    }
                }
            }
        }

        if (entry.reflection.isNotBlank()) {
            Text(
                entry.reflection,
                style = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, color = BodyText),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JournalContentPreview() {
    OneTheme {
        JournalContent(
            entries = listOf(
                DiaryEntry(
                    id = "1",
                    date = DiaryDateKeys.todayKey(),
                    mood = "💪",
                    energyLevel = 8,
                    sleepHours = 7,
                    soreness = mapOf("quads" to 2, "shoulders" to 1),
                    reflection = "Today's biggest win: hit a new squat PR."
                ),
                DiaryEntry(
                    id = "2",
                    date = "2026-09-12",
                    mood = "😴",
                    energyLevel = 4,
                    sleepHours = 5,
                    reflection = "Held back by poor sleep."
                )
            ),
            onWriteTodayClick = {},
            onEditEntry = {},
            onDeleteEntry = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun JournalContentEmptyPreview() {
    OneTheme {
        JournalContent(entries = emptyList(), onWriteTodayClick = {}, onEditEntry = {}, onDeleteEntry = {})
    }
}
