package app.theone.android.settings.frontend

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.theone.android.meals.backend.MealSettings
import app.theone.android.notifications.WorkoutDoNotDisturb
import app.theone.android.notifications.dndAccessSettingsIntent
import app.theone.android.settings.backend.DataStoreSettingsRepository
import app.theone.android.settings.backend.SettingsViewModel
import app.theone.android.settings.backend.ThemeMode
import app.theone.android.settings.backend.WeekStart
import app.theone.android.ui.BackTopBar
import app.theone.android.ui.ErrorBanner
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.NeutralSelected
import app.theone.android.ui.theme.NeutralSwitchOff
import app.theone.android.ui.theme.NeutralSwitchOnThumb
import app.theone.android.ui.theme.NeutralTheme
import app.theone.android.ui.theme.OneTheme
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDisabledFill
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface
import kotlinx.coroutines.launch

// 1d / 2d in the home handoff.

/** A [SettingsViewModel] on the device's DataStore - also used by WorkoutScreen for the rest-bar vibration. */
@Composable
fun settingsViewModel(): SettingsViewModel = viewModel(
    factory = viewModelFactory {
        initializer { SettingsViewModel(DataStoreSettingsRepository(checkNotNull(this[APPLICATION_KEY]))) }
    }
)

/** Settings: the list of sections (#62). Each row opens [SettingsSectionScreen]. */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenSection: (SettingsSection) -> Unit, viewModel: SettingsViewModel = settingsViewModel()) {
    val themeMode by viewModel.themeMode.collectAsState()
    val weekStart by viewModel.weekStart.collectAsState()
    val vibrateOnRestEnd by viewModel.vibrateOnRestEnd.collectAsState()
    val dndDuringWorkout by viewModel.dndDuringWorkout.collectAsState()
    val mealSettings by viewModel.mealSettings.collectAsState()
    val settingsError by viewModel.settingsError.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsListContent(
            themeMode = themeMode,
            onThemeModeChange = viewModel::setThemeMode,
            weekStart = weekStart,
            onWeekStartChange = viewModel::setWeekStart,
            summaries = mapOf(
                SettingsSection.CALENDAR to NO_SETTINGS_YET,
                SettingsSection.WORKOUT to workoutSummary(vibrateOnRestEnd, dndDuringWorkout),
                SettingsSection.MEALS to mealsSummary(mealSettings),
                SettingsSection.DIARY to NO_SETTINGS_YET
            ),
            onBack = onBack,
            onOpenSection = onOpenSection
        )
        ErrorBanner(message = settingsError, onDismiss = viewModel::clearSettingsError, modifier = Modifier.align(Alignment.TopCenter))
    }
}

/** One Settings section on its own screen: the same controls as before the split, moved as they are. */
@Composable
fun SettingsSectionScreen(section: SettingsSection, onBack: () -> Unit, viewModel: SettingsViewModel = settingsViewModel()) {
    val themeMode by viewModel.themeMode.collectAsState()
    val vibrateOnRestEnd by viewModel.vibrateOnRestEnd.collectAsState()
    val dndDuringWorkout by viewModel.dndDuringWorkout.collectAsState()
    val mealSettings by viewModel.mealSettings.collectAsState()
    val settingsError by viewModel.settingsError.collectAsState()

    // Do Not Disturb needs access the user grants in system settings (#65): turning the switch on without
    // it opens that screen, and the setting is stored on return only if access was granted.
    val context = LocalContext.current
    val workoutDnd = remember(context) { WorkoutDoNotDisturb(context) }
    val scope = rememberCoroutineScope()
    val accessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (workoutDnd.hasAccess()) viewModel.setDndDuringWorkout(true)
    }
    val onDndDuringWorkoutChange: (Boolean) -> Unit = { enabled ->
        when {
            !enabled -> {
                viewModel.setDndDuringWorkout(false)
                scope.launch { workoutDnd.restore() } // also mid-workout
            }
            workoutDnd.hasAccess() -> viewModel.setDndDuringWorkout(true)
            else -> accessLauncher.launch(dndAccessSettingsIntent())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsSectionContent(
            section = section,
            themeMode = themeMode,
            onThemeModeChange = viewModel::setThemeMode,
            vibrateOnRestEnd = vibrateOnRestEnd,
            onVibrateOnRestEndChange = viewModel::setVibrateOnRestEnd,
            dndDuringWorkout = dndDuringWorkout,
            onDndDuringWorkoutChange = onDndDuringWorkoutChange,
            onBack = onBack,
            mealSettings = mealSettings,
            onMealSettingsChange = viewModel::setMealSettings
        )
        ErrorBanner(message = settingsError, onDismiss = viewModel::clearSettingsError, modifier = Modifier.align(Alignment.TopCenter))
    }
}

private val CardShape = RoundedCornerShape(20.dp)

/** App-wide settings first (Appearance -> Theme, General -> Week starts on), then a row per app section (#62). */
@Composable
private fun SettingsListContent(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    weekStart: WeekStart,
    onWeekStartChange: (WeekStart) -> Unit,
    summaries: Map<SettingsSection, String>,
    onBack: () -> Unit,
    onOpenSection: (SettingsSection) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(WarmSurface)) {
        BackTopBar(title = "Settings", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionLabel("Appearance")
            ThemeCard(themeMode = themeMode, onThemeModeChange = onThemeModeChange)
            SectionLabel("General", top = 12.dp)
            WeekStartCard(weekStart = weekStart, onChange = onWeekStartChange)
            SectionLabel("Sections", top = 12.dp)
            SettingsSectionList(summaries = summaries, onOpen = onOpenSection)
        }
    }
}

@Composable
private fun SectionLabel(text: String, top: Dp = 4.dp) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = WarmSecondaryText,
        modifier = Modifier.padding(start = 8.dp, top = top, end = 8.dp)
    )
}

/** Calendar and Diary have nothing to set yet. */
@Composable
private fun NoSettingsNote(section: SettingsSection) {
    Text(
        "No ${section.title} settings yet.",
        fontSize = 14.sp,
        color = WarmSecondaryText,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
    )
}

@Composable
private fun SettingsSectionContent(
    section: SettingsSection,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    vibrateOnRestEnd: Boolean,
    onVibrateOnRestEndChange: (Boolean) -> Unit,
    dndDuringWorkout: Boolean,
    onDndDuringWorkoutChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    mealSettings: MealSettings = MealSettings(),
    onMealSettingsChange: (MealSettings) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSurface)
    ) {
        BackTopBar(title = section.title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Edge-to-edge doesn't resize the window for the keyboard: shrink the scroll area
                // instead, so a focused field (e.g. rest-day targets) scrolls above the number pad.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (section) {
                SettingsSection.WORKOUT -> {
                    SwitchRow(
                        icon = Icons.Outlined.Vibration,
                        title = "Vibrate when rest ends",
                        subtitle = "Buzz when the rest timer reaches zero",
                        checked = vibrateOnRestEnd,
                        onCheckedChange = onVibrateOnRestEndChange
                    )
                    SwitchRow(
                        icon = Icons.Outlined.DoNotDisturbOn,
                        title = "Do Not Disturb during workouts",
                        subtitle = "Priority only while a workout runs, then back to how it was",
                        checked = dndDuringWorkout,
                        onCheckedChange = onDndDuringWorkoutChange
                    )
                }
                SettingsSection.MEALS -> MealSettingsSection(settings = mealSettings, onChange = onMealSettingsChange)
                SettingsSection.CALENDAR, SettingsSection.DIARY -> NoSettingsNote(section)
            }
        }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CardSurface)
            .border(1.dp, WarmCardBorder, CardShape)
            // The whole row toggles the switch.
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = WarmIcon, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = WarmOnSurface)
            Text(subtitle, fontSize = 13.sp, color = WarmSecondaryText)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = WarmOnSurface,
                checkedThumbColor = NeutralSwitchOnThumb,
                checkedBorderColor = WarmOnSurface,
                uncheckedTrackColor = WarmDisabledFill,
                uncheckedThumbColor = NeutralSwitchOff,
                uncheckedBorderColor = NeutralSwitchOff
            )
        )
    }
}

private val SegmentedShape = RoundedCornerShape(20.dp)

private data class ThemeOption(val mode: ThemeMode, val label: String, val icon: ImageVector)

private val ThemeOptions = listOf(
    ThemeOption(ThemeMode.LIGHT, "Light", Icons.Outlined.LightMode),
    ThemeOption(ThemeMode.DARK, "Dark", Icons.Outlined.DarkMode),
    ThemeOption(ThemeMode.SYSTEM, "System", Icons.Outlined.Contrast)
)

/** Appearance -> Theme: a 3-way segmented control; the selected segment shows a check instead of its icon. */
@Composable
private fun ThemeCard(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CardSurface)
            .border(1.dp, WarmCardBorder, CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Contrast, contentDescription = null, tint = WarmIcon, modifier = Modifier.size(24.dp))
            Text("Theme", fontSize = 16.sp, color = WarmOnSurface)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(SegmentedShape)
                .border(1.dp, WarmButtonOutline, SegmentedShape)
                .selectableGroup()
        ) {
            ThemeOptions.forEachIndexed { index, option ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(WarmButtonOutline)
                    )
                }
                val selected = option.mode == themeMode
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (selected) NeutralSelected else CardSurface)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onThemeModeChange(option.mode) }),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (selected) Icons.Outlined.Check else option.icon,
                        contentDescription = null,
                        tint = WarmOnSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        option.label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = WarmOnSurface
                    )
                }
            }
        }
    }
}

private val previewSummaries = mapOf(
    SettingsSection.CALENDAR to NO_SETTINGS_YET,
    SettingsSection.WORKOUT to workoutSummary(true, false),
    SettingsSection.MEALS to mealsSummary(MealSettings()),
    SettingsSection.DIARY to NO_SETTINGS_YET
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun SettingsScreenPreview() {
    NeutralTheme {
        SettingsListContent(ThemeMode.SYSTEM, {}, WeekStart.MONDAY, {}, previewSummaries, onBack = {}, onOpenSection = {})
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun SettingsScreenDarkPreview() {
    OneTheme(darkTheme = true) {
        NeutralTheme {
            SettingsListContent(ThemeMode.DARK, {}, WeekStart.SUNDAY, {}, previewSummaries, onBack = {}, onOpenSection = {})
        }
    }
}
