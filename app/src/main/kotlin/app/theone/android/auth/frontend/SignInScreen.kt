package app.theone.android.auth.frontend

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.theone.android.R
import app.theone.android.auth.backend.AuthViewModel
import app.theone.android.ui.theme.MonochromeTheme

@Composable
fun SignInScreen(viewModel: AuthViewModel) {
    val error by viewModel.authError.collectAsState()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }

    MonochromeTheme {
        SignInScreenContent(
            email = email,
            password = password,
            isSignUpMode = isSignUpMode,
            error = error,
            onEmailChange = { email = it },
            onPasswordChange = { password = it },
            onToggleMode = { isSignUpMode = !isSignUpMode },
            onSubmit = {
                if (isSignUpMode) {
                    viewModel.signUpWithEmail(email, password)
                } else {
                    viewModel.signInWithEmail(email, password)
                }
            },
            onGoogleClick = { viewModel.signInWithGoogle(context) }
        )
    }
}

@Composable
private fun SignInScreenContent(
    email: String,
    password: String,
    isSignUpMode: Boolean,
    error: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onGoogleClick: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = stringResource(R.string.auth_logo_content_description),
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.auth_email_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.auth_password_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (isSignUpMode) R.string.auth_create_account else R.string.auth_sign_in))
            }
            TextButton(onClick = onToggleMode) {
                Text(
                    stringResource(
                        if (isSignUpMode) {
                            R.string.auth_toggle_to_sign_in
                        } else {
                            R.string.auth_toggle_to_sign_up
                        }
                    )
                )
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(stringResource(R.string.auth_or_divider))
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(onClick = onGoogleClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.auth_continue_with_google))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignInScreenPreview() {
    MonochromeTheme {
        SignInScreenContent(
            email = "",
            password = "",
            isSignUpMode = false,
            error = null,
            onEmailChange = {},
            onPasswordChange = {},
            onToggleMode = {},
            onSubmit = {},
            onGoogleClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Sign up mode")
@Composable
private fun SignInScreenSignUpPreview() {
    MonochromeTheme {
        SignInScreenContent(
            email = "user@example.com",
            password = "",
            isSignUpMode = true,
            error = null,
            onEmailChange = {},
            onPasswordChange = {},
            onToggleMode = {},
            onSubmit = {},
            onGoogleClick = {}
        )
    }
}

@Preview(showBackground = true, name = "With error")
@Composable
private fun SignInScreenErrorPreview() {
    MonochromeTheme {
        SignInScreenContent(
            email = "user@example.com",
            password = "",
            isSignUpMode = false,
            error = "Sign-in failed. Please try again.",
            onEmailChange = {},
            onPasswordChange = {},
            onToggleMode = {},
            onSubmit = {},
            onGoogleClick = {}
        )
    }
}
