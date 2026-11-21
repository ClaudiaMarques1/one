package app.theone.android.auth.frontend

import app.theone.android.auth.backend.SignInMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountLabelsTest {
    @Test
    fun `sign-in method labels`() {
        assertEquals("Google", SignInMethod.GOOGLE.label())
        assertEquals("Email and password", SignInMethod.EMAIL_PASSWORD.label())
        assertEquals("Other", SignInMethod.OTHER.label())
    }

    @Test
    fun `a blank name is Not set`() {
        assertEquals("Not set", nameLabel(""))
        assertEquals("Not set", nameLabel("  "))
        assertEquals("Ada Lovelace", nameLabel("Ada Lovelace"))
    }
}
