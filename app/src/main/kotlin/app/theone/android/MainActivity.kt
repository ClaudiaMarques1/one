package app.theone.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.theone.android.auth.backend.AuthViewModel
import app.theone.android.auth.frontend.SignInScreen
import app.theone.android.navigation.OneNavHost
import app.theone.android.settings.backend.isDark
import app.theone.android.settings.frontend.settingsViewModel
import app.theone.android.ui.theme.OneTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Settings -> Appearance -> Theme; System follows the phone, as before the setting existed.
            val themeMode by settingsViewModel().themeMode.collectAsState()
            val darkTheme = themeMode.isDark(systemDark = isSystemInDarkTheme())
            // enableEdgeToEdge() alone picks status-bar icon colours from the phone's mode, which would
            // leave white icons on a light screen when the app is set to Light on a dark phone.
            LaunchedEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            OneTheme(darkTheme = darkTheme) {
                val currentUser by authViewModel.currentUser.collectAsState()
                if (currentUser == null) {
                    SignInScreen(viewModel = authViewModel)
                } else {
                    OneNavHost(authViewModel = authViewModel)
                }
            }
        }
    }
}
