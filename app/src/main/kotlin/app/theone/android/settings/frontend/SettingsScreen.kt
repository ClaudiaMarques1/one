package app.theone.android.settings.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.theone.android.settings.backend.DataStoreSettingsRepository
import app.theone.android.settings.backend.SettingsViewModel
import app.theone.android.ui.BackTopBar
import app.theone.android.ui.ErrorBanner
import app.theone.android.ui.theme.NeutralSwitchOff
import app.theone.android.ui.theme.NeutralTheme
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDisabledFill
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface

// 1d in the home handoff, light only. The Theme row (Light / Dark / System) comes with dark mode (idea #3).

/** A [SettingsViewModel] on the device's DataStore - also used by WorkoutScreen for the rest-bar vibration. */
@Composable
fun settingsViewModel(): SettingsViewModel = viewModel(
    factory = viewModelFactory {
        initializer { SettingsViewModel(DataStoreSettingsRepository(checkNotNull(this[APPLICATION_KEY]))) }
    }
)

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = settingsViewModel()) {
    val vibrateOnRestEnd by viewModel.vibrateOnRestEnd.collectAsState()
    val settingsError by viewModel.settingsError.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsScreenContent(
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
private fun SettingsScreenContent(vibrateOnRestEnd: Boolean, onVibrateOnRestEndChange: (Boolean) -> Unit, onBack: () -> Unit) {
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
            SectionLabel("Workout")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Color.White)
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
                        checkedThumbColor = Color.White,
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

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = WarmSecondaryText,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 8.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    NeutralTheme {
        SettingsScreenContent(vibrateOnRestEnd = true, onVibrateOnRestEndChange = {}, onBack = {})
    }
}
