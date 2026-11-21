package app.theone.android.auth.backend

import com.google.firebase.auth.FirebaseUser

// The signed-in user as the screens see it - plain values, so no composable imports a Firebase class.

enum class SignInMethod { GOOGLE, EMAIL_PASSWORD, OTHER }

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val signInMethod: SignInMethod = SignInMethod.OTHER
)

/** From Firebase provider ids ("google.com", "password"); Google wins if an account has both. */
fun signInMethodOf(providerIds: List<String>): SignInMethod = when {
    "google.com" in providerIds -> SignInMethod.GOOGLE
    "password" in providerIds -> SignInMethod.EMAIL_PASSWORD
    else -> SignInMethod.OTHER
}

fun FirebaseUser.toUserProfile(): UserProfile = UserProfile(
    name = displayName.orEmpty(),
    email = email.orEmpty(),
    photoUrl = photoUrl?.toString(),
    signInMethod = signInMethodOf(providerData.map { it.providerId })
)
