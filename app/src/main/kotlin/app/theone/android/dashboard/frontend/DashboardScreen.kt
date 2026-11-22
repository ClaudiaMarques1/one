package app.theone.android.dashboard.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.theone.android.R
import app.theone.android.auth.backend.UserProfile
import app.theone.android.diary.journal.backend.DiaryEntry
import app.theone.android.diary.journal.backend.DiaryViewModel
import app.theone.android.ui.ProfileAvatar
import app.theone.android.ui.formatDisplayDate
import app.theone.android.ui.profileInitials
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.NeutralIconTile
import app.theone.android.ui.theme.NeutralTheme
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface

// 1a in the home handoff. The avatar opens the profile drawer, which OneNavHost owns.

@Composable
fun DashboardScreen(profile: UserProfile?, onAvatarClick: () -> Unit, diaryViewModel: DiaryViewModel = viewModel()) {
    val entries by diaryViewModel.entries.collectAsState()

    DashboardScreenContent(
        profile = profile,
        diaryEntryCount = entries.size,
        latestDiaryEntry = entries.firstOrNull(),
        onAvatarClick = onAvatarClick
    )
}

private data class DashboardCardData(
    val title: String,
    val icon: ImageVector,
    val summary: String
)

private val CardShape = RoundedCornerShape(20.dp)

@Composable
private fun DashboardScreenContent(
    profile: UserProfile?,
    diaryEntryCount: Int,
    latestDiaryEntry: DiaryEntry?,
    onAvatarClick: () -> Unit
) {
    val comingSoon = stringResource(R.string.dashboard_coming_soon)
    val diarySummary = if (diaryEntryCount == 0) {
        stringResource(R.string.dashboard_no_entries)
    } else {
        pluralStringResource(R.plurals.dashboard_entry_count, diaryEntryCount, diaryEntryCount) +
            (latestDiaryEntry?.let { stringResource(R.string.dashboard_latest_suffix, it.mood, formatDisplayDate(it.date)) } ?: "")
    }

    val cards = listOf(
        DashboardCardData(stringResource(R.string.dashboard_card_diary), Icons.Outlined.Book, diarySummary),
        DashboardCardData(stringResource(R.string.dashboard_card_workout), Icons.Outlined.FitnessCenter, comingSoon),
        DashboardCardData(stringResource(R.string.dashboard_card_meals), Icons.Outlined.Restaurant, comingSoon),
        DashboardCardData(stringResource(R.string.dashboard_card_calendar), Icons.Outlined.CalendarMonth, comingSoon)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val openProfile = stringResource(R.string.dashboard_open_profile)
            // 40dp avatar in a 48dp touch target.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClickLabel = openProfile, role = Role.Button, onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                ProfileAvatar(
                    photoUrl = profile?.photoUrl,
                    initials = profileInitials(profile?.name.orEmpty(), profile?.email.orEmpty()),
                    size = 40.dp,
                    initialsSize = 15.sp
                )
            }
            Text(
                stringResource(R.string.dashboard_overview_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = WarmOnSurface
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cards) { card -> DashboardCard(card) }
        }
    }
}

@Composable
private fun DashboardCard(card: DashboardCardData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CardSurface)
            .border(1.dp, WarmCardBorder, CardShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(NeutralIconTile),
            contentAlignment = Alignment.Center
        ) {
            Icon(card.icon, contentDescription = null, tint = WarmOnSurface, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(card.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface)
            Text(card.summary, fontSize = 14.sp, color = WarmSecondaryText, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    NeutralTheme {
        DashboardScreenContent(
            profile = UserProfile(name = "Claudia Marques", email = "claudia@example.com"),
            diaryEntryCount = 2,
            latestDiaryEntry = DiaryEntry(id = "1", date = "2026-09-12", mood = "💪"),
            onAvatarClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun DashboardScreenEmptyPreview() {
    NeutralTheme {
        DashboardScreenContent(profile = null, diaryEntryCount = 0, latestDiaryEntry = null, onAvatarClick = {})
    }
}
