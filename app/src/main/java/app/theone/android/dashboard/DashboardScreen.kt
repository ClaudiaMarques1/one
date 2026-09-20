package app.theone.android.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.theone.android.diary.DiaryEntry
import app.theone.android.diary.DiaryViewModel
import app.theone.android.ui.theme.OneTheme

@Composable
fun DashboardScreen(onSignOut: () -> Unit, diaryViewModel: DiaryViewModel = viewModel()) {
    val entries by diaryViewModel.entries.collectAsState()

    DashboardScreenContent(
        diaryEntryCount = entries.size,
        latestDiaryEntry = entries.firstOrNull(),
        onSignOut = onSignOut
    )
}

private data class DashboardCardData(
    val title: String,
    val icon: ImageVector,
    val summary: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreenContent(
    diaryEntryCount: Int,
    latestDiaryEntry: DiaryEntry?,
    onSignOut: () -> Unit
) {
    val diarySummary = if (diaryEntryCount == 0) {
        "No entries yet"
    } else {
        val noun = if (diaryEntryCount == 1) "entry" else "entries"
        "$diaryEntryCount $noun" + (latestDiaryEntry?.let { " · latest: ${it.mood} ${it.date}" } ?: "")
    }

    val cards = listOf(
        DashboardCardData("Diary", Icons.Filled.Book, diarySummary),
        DashboardCardData("Workout", Icons.Filled.FitnessCenter, "Coming soon"),
        DashboardCardData("Meals", Icons.Filled.Restaurant, "Coming soon"),
        DashboardCardData("Calendar", Icons.Filled.CalendarMonth, "Coming soon")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overview") },
                actions = { TextButton(onClick = onSignOut) { Text("Sign out") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cards) { card -> DashboardCard(card) }
        }
    }
}

@Composable
private fun DashboardCard(card: DashboardCardData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(card.icon, contentDescription = null)
            Column {
                Text(card.title, style = MaterialTheme.typography.titleMedium)
                Text(card.summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    OneTheme {
        DashboardScreenContent(
            diaryEntryCount = 2,
            latestDiaryEntry = DiaryEntry(id = "1", date = "2026-09-12", mood = "💪"),
            onSignOut = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun DashboardScreenEmptyPreview() {
    OneTheme {
        DashboardScreenContent(diaryEntryCount = 0, latestDiaryEntry = null, onSignOut = {})
    }
}
