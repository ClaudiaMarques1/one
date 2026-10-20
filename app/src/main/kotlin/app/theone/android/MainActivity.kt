package app.theone.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.theone.android.auth.backend.AuthViewModel
import app.theone.android.auth.frontend.SignInScreen
import app.theone.android.navigation.OneNavHost
import app.theone.android.ui.theme.OneTheme

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OneTheme {
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
