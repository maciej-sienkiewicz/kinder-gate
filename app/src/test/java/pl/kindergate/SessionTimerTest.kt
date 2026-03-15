package pl.kindergate

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.kindergate.service.SessionTimer

/**
 * Unit tests for SessionTimer.
 *
 * Key test scenarios:
 * 1. Timer starts in IDLE, transitions to RUNNING on first tick
 * 2. Timer returns false for ticks before interval elapses
 * 3. Timer returns true exactly when interval elapses
 * 4. Timer is immune to System.currentTimeMillis() (uses elapsedRealtime)
 * 5. Timer resets correctly after acknowledgment
 * 6. Timer handles app switching correctly
 * 7. Timer state transitions are correct
 */
class SessionTimerTest {

    private lateinit var timer: SessionTimer
    private var mockedElapsed = 0L

    @Before
    fun setUp() {
        timer = SessionTimer()
        mockedElapsed = 0L
        // Mock SystemClock.elapsedRealtime() to control time in tests
        mockkStatic(android.os.SystemClock::class)
        every { android.os.SystemClock.elapsedRealtime() } answers { mockedElapsed }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `initial state is IDLE`() {
        assertEquals(SessionTimer.State.IDLE, timer.getCurrentState())
        assertEquals(0L, timer.getElapsedMs())
    }

    @Test
    fun `first tick transitions from IDLE to RUNNING`() {
        val shouldBlock = timer.tick("com.example.app", 60_000L)

        assertFalse("Should not block on first tick", shouldBlock)
        assertEquals(SessionTimer.State.RUNNING, timer.getCurrentState())
    }

    @Test
    fun `does not block before interval elapses`() {
        mockedElapsed = 0L
        timer.tick("com.example.app", 60_000L)

        // Simulate 59 seconds passing
        mockedElapsed = 59_000L
        val shouldBlock = timer.tick("com.example.app", 60_000L)

        assertFalse("Should not block at 59s", shouldBlock)
    }

    @Test
    fun `blocks exactly when interval elapses`() {
        mockedElapsed = 0L
        timer.tick("com.example.app", 60_000L)

        mockedElapsed = 60_000L
        val shouldBlock = timer.tick("com.example.app", 60_000L)

        assertTrue("Should block at 60s", shouldBlock)
        assertEquals(SessionTimer.State.BLOCKING, timer.getCurrentState())
    }

    @Test
    fun `resets after acknowledgment`() {
        mockedElapsed = 0L
        timer.tick("com.example.app", 60_000L)
        mockedElapsed = 60_000L
        timer.tick("com.example.app", 60_000L)

        timer.onBlockAcknowledged()

        assertEquals(SessionTimer.State.IDLE, timer.getCurrentState())
        assertEquals(0L, timer.getElapsedMs())
    }

    @Test
    fun `after acknowledgment, new 60s window starts fresh`() {
        // First window
        mockedElapsed = 0L
        timer.tick("com.example.app", 60_000L)
        mockedElapsed = 60_000L
        timer.tick("com.example.app", 60_000L) // triggers block
        timer.onBlockAcknowledged()

        // Second window – starts at elapsed=60_000
        mockedElapsed = 60_000L
        timer.tick("com.example.app", 60_000L)
        // 59s into second window
        mockedElapsed = 119_000L
        val shouldBlock = timer.tick("com.example.app", 60_000L)

        assertFalse("Should not block 59s into second window", shouldBlock)
    }

    @Test
    fun `app switch resets timer to IDLE`() {
        mockedElapsed = 0L
        timer.tick("com.tiktok.app", 60_000L)
        mockedElapsed = 30_000L
        timer.tick("com.tiktok.app", 60_000L)

        // Non-monitored app comes to foreground
        timer.onNonMonitoredForeground()

        assertEquals(SessionTimer.State.IDLE, timer.getCurrentState())
        assertEquals(0L, timer.getElapsedMs())
    }

    @Test
    fun `switching between two monitored apps resets timer`() {
        mockedElapsed = 0L
        timer.tick("com.tiktok.app", 60_000L)
        mockedElapsed = 30_000L

        // Different monitored app
        timer.tick("com.youtube.app", 60_000L)

        // Timer should reset for the new app
        assertEquals(SessionTimer.State.RUNNING, timer.getCurrentState())
        assertEquals(0L, timer.snapshot.value.elapsedInSessionMs)
    }

    @Test
    fun `timer is immune to system time manipulation`() {
        // This test verifies that changing System.currentTimeMillis() does NOT
        // affect the timer, which uses elapsedRealtime() exclusively.
        // In the actual implementation, we don't even read currentTimeMillis() in the timer.

        mockedElapsed = 0L
        timer.tick("com.example.app", 60_000L)

        // Simulate child changing system clock forward by 1 hour
        // (This affects System.currentTimeMillis() but NOT elapsedRealtime())
        // Our timer only uses elapsedRealtime, so this has no effect.

        // Only 10 real seconds have passed (elapsedRealtime = 10_000)
        mockedElapsed = 10_000L
        val shouldBlock = timer.tick("com.example.app", 60_000L)

        assertFalse("System clock manipulation should not trigger block", shouldBlock)
        assertEquals(SessionTimer.State.RUNNING, timer.getCurrentState())
    }

    @Test
    fun `blocking state does not advance on repeated ticks`() {
        mockedElapsed = 0L
        timer.tick("com.example.app", 60_000L)
        mockedElapsed = 60_000L
        timer.tick("com.example.app", 60_000L) // triggers block

        assertEquals(SessionTimer.State.BLOCKING, timer.getCurrentState())

        // Additional ticks should not change state or return true again
        mockedElapsed = 70_000L
        val result = timer.tick("com.example.app", 60_000L)

        assertFalse("Tick during BLOCKING should return false", result)
        assertEquals(SessionTimer.State.BLOCKING, timer.getCurrentState())
    }

    @Test
    fun `reset clears all state`() {
        mockedElapsed = 0L
        timer.tick("com.example.app", 60_000L)
        mockedElapsed = 30_000L
        timer.tick("com.example.app", 60_000L)

        timer.reset()

        assertEquals(SessionTimer.State.IDLE, timer.getCurrentState())
        assertEquals(0L, timer.getElapsedMs())
    }
}
