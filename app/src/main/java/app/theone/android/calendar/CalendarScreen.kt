package app.theone.android.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.theone.android.navigation.Destination
import app.theone.android.ui.ErrorBanner
import app.theone.android.ui.theme.OneTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val WorkoutDotColor = Destination.Workout.accentColor!!
private val DiaryDotColor = Destination.Diary.accentColor!!

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val activityByDate by viewModel.activityByDate.collectAsState()
    val calendarError by viewModel.calendarError.collectAsState()
    var year by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }

    CalendarScreenContent(
        year = year,
        month = month,
        activityByDate = activityByDate,
        errorMessage = calendarError,
        onDismissError = { viewModel.clearCalendarError() },
        onPreviousMonth = {
            if (month == 0) {
                month = 11
                year--
            } else {
                month--
            }
        },
        onNextMonth = {
            if (month == 11) {
                month = 0
                year++
            } else {
                month++
            }
        }
    )
}

@Composable
private fun CalendarScreenContent(
    year: Int,
    month: Int,
    activityByDate: Map<String, Set<ActivityType>>,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dateKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    val monthCalendar = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingBlankDays = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1

    val today = remember { Calendar.getInstance() }
    val isCurrentMonth = today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == month
    val todayDayOfMonth = today.get(Calendar.DAY_OF_MONTH)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            ErrorBanner(message = errorMessage, onDismiss = onDismissError)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                }
                Text(monthFormat.format(monthCalendar.time), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth()) {
                items(leadingBlankDays) {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
                items(daysInMonth) { index ->
                    val day = index + 1
                    val dayCalendar = (monthCalendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                    val dateKey = dateKeyFormat.format(dayCalendar.time)
                    val activities = activityByDate[dateKey].orEmpty()
                    val isToday = isCurrentMonth && day == todayDayOfMonth

                    DayCell(day = day, activities = activities, isToday = isToday)
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, activities: Set<ActivityType>, isToday: Boolean) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .then(
                    if (isToday) {
                        Modifier.background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                day.toString(),
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (ActivityType.WORKOUT in activities) Dot(WorkoutDotColor)
            if (ActivityType.DIARY in activities) Dot(DiaryDotColor)
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(color, shape = CircleShape)
    )
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = Calendar.getInstance()
    val sample = mapOf(
        format.format(today.time) to setOf(ActivityType.WORKOUT, ActivityType.DIARY)
    )
    OneTheme {
        CalendarScreenContent(
            year = today.get(Calendar.YEAR),
            month = today.get(Calendar.MONTH),
            activityByDate = sample,
            onPreviousMonth = {},
            onNextMonth = {}
        )
    }
}
