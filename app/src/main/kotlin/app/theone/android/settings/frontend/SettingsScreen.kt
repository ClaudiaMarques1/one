package app.theone.android.settings.frontend

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
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import app.theone.android.settings.backend.DataStoreSettingsRepository
import app.theone.android.settings.backend.SettingsViewModel
import app.theone.android.settings.backend.ThemeMode
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

// 1d / 2d in the home handoff.

/** A [SettingsViewModel] on the device's DataStore - also used by WorkoutScreen for the rest-bar vibration. */
@Composable
fun settingsViewModel(): SettingsViewModel = viewModel(
    factory = viewModelFactory {
        initializer { SettingsViewModel(DataStoreSettingsRepository(checkNotNull(this[APPLICATION_KEY]))) }
    }
)

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = settingsViewModel()) {
    val themeMode by viewModel.themeMode.collectAsState()
    val vibrateOnRestEnd by viewModel.vibrateOnRestEnd.collectAsState()
    val settingsError by viewModel.settingsError.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsScreenContent(
            themeMode = themeMode,
            onThemeModeChange = viewModel::setThemeMode,
            vibrateOnRestEnd = vibrateOnRestEnd,
            onVibrateOnRestEndChange = viewModel::setVibrateOnRestEnd,
            onBack = onBack
        )
        ErrorBanner(
            message = settingsError,
            onDismiss = viewModel::clearSettingsError,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private val CardShape = RoundedCornerShape(20.dp)

@Composable
private fun SettingsScreenContent(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    vibrateOnRestEnd: Boolean,
    onVibrateOnRestEndChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSurface)
    ) {
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
            SectionLabel("Workout", top = 12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(CardSurface)
                    .border(1.dp, WarmCardBorder, CardShape)
                    // The whole row toggles the switch.
                    .toggleable(value = vibrateOnRestEnd, role = Role.Switch, onValueChange = onVibrateOnRestEndChange)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Vibration, contentDescription = null, tint = WarmIcon, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vibrate when rest ends", fontSize = 16.sp, color = WarmOnSurface)
                    Text("Buzz when the rest timer reaches zero", fontSize = 13.sp, color = WarmSecondaryText)
                }
                Switch(
                    checked = vibrateOnRestEnd,
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    NeutralTheme {
        SettingsScreenContent(
            themeMode = ThemeMode.SYSTEM,
            onThemeModeChange = {},
            vibrateOnRestEnd = true,
            onVibrateOnRestEndChange = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    OneTheme(darkTheme = true) {
        NeutralTheme {
            SettingsScreenContent(
                themeMode = ThemeMode.DARK,
                onThemeModeChange = {},
                vibrateOnRestEnd = false,
                onVibrateOnRestEndChange = {},
                onBack = {}
            )
        }
    }
}
