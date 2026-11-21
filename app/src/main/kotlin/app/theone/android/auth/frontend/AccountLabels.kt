package app.theone.android.auth.frontend

import app.theone.android.auth.backend.SignInMethod

// Display text for Account details.

internal fun SignInMethod.label(): String = when (this) {
    SignInMethod.GOOGLE -> "Google"
    SignInMethod.EMAIL_PASSWORD -> "Email and password"
    SignInMethod.OTHER -> "Other"
}

/** A blank name shows as "Not set". */
internal fun nameLabel(name: String): String = name.ifBlank { "Not set" }
