package app.theone.android.auth.backend

import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileTest {
    @Test
    fun `signInMethodOf maps Firebase provider ids`() {
        assertEquals(SignInMethod.GOOGLE, signInMethodOf(listOf("firebase", "google.com")))
        assertEquals(SignInMethod.EMAIL_PASSWORD, signInMethodOf(listOf("firebase", "password")))
        assertEquals(SignInMethod.OTHER, signInMethodOf(listOf("firebase")))
    }

    @Test
    fun `signInMethodOf prefers Google when both are linked`() {
        assertEquals(SignInMethod.GOOGLE, signInMethodOf(listOf("password", "google.com")))
    }
}
