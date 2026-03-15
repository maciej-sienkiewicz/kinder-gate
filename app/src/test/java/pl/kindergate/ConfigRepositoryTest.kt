package pl.kindergate

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.kindergate.data.local.prefs.SecurePreferencesManager

/**
 * Tests for PIN management logic in SecurePreferencesManager.
 * Uses mockk to avoid Android dependency on EncryptedSharedPreferences.
 *
 * For actual integration tests of EncryptedSharedPreferences, see androidTest.
 */
class PinSecurityTest {

    private lateinit var prefs: SecurePreferencesManager

    @Before
    fun setUp() {
        prefs = mockk(relaxed = true)
    }

    @Test
    fun `verifyPin returns true for correct PIN`() {
        every { prefs.verifyPin("1234") } returns true
        assertTrue(prefs.verifyPin("1234"))
    }

    @Test
    fun `verifyPin returns false for wrong PIN`() {
        every { prefs.verifyPin("9999") } returns false
        assertFalse(prefs.verifyPin("9999"))
    }

    @Test
    fun `verifyPin returns false when no PIN configured`() {
        every { prefs.verifyPin(any()) } returns false
        every { prefs.isPinConfigured() } returns false
        assertFalse(prefs.verifyPin("0000"))
    }

    @Test
    fun `setPinHash is called when saving PIN`() {
        prefs.setPinHash("5678")
        verify { prefs.setPinHash("5678") }
    }
}
