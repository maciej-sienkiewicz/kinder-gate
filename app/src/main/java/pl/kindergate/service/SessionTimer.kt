package pl.kindergate.service

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks active usage time for the currently monitored foreground application.
 *
 * CRITICAL DESIGN DECISION: Uses SystemClock.elapsedRealtime() exclusively.
 *
 * elapsedRealtime() is a monotonic clock that:
 * - Starts at device boot, not epoch
 * - Continues to tick during sleep (unlike uptimeMillis)
 * - CANNOT be manipulated by changing device time in Settings
 * - Is unaffected by timezone changes, DST, or NTP adjustments
 *
 * This is the correct clock for measuring "how long did the child use this app"
 * because System.currentTimeMillis() can be manipulated by the user.
 *
 * Timer state machine:
 *   IDLE -> RUNNING (when monitored app comes to foreground)
 *   RUNNING -> BLOCKING (when intervalMs elapsed)
 *   BLOCKING -> IDLE (when child presses OK or navigates away)
 *   RUNNING -> PAUSED (when app goes to background, not yet implemented in MVP)
 *
 * For MVP: timer is reset to 0 after each block acknowledgment. Per-day accumulation
 * is implemented in v1.
 */
@Singleton
class SessionTimer @Inject constructor() {

    enum class State { IDLE, RUNNING, BLOCKING }

    data class TimerSnapshot(
        val state: State,
        val activePackage: String?,
        val elapsedInSessionMs: Long,  // monotonic time accumulated in this 60s window
        val sessionStartElapsedMs: Long // when current window started (for recovery)
    )

    private val _snapshot = MutableStateFlow(
        TimerSnapshot(
            state = State.IDLE,
            activePackage = null,
            elapsedInSessionMs = 0L,
            sessionStartElapsedMs = 0L
        )
    )
    val snapshot: StateFlow<TimerSnapshot> = _snapshot.asStateFlow()

    // Tracks when this 60s window started, using elapsedRealtime
    private var windowStartElapsedMs: Long = 0L

    /**
     * Called each detection tick when a monitored app is in foreground.
     * Returns true if the block threshold has been reached.
     *
     * [intervalMs] is sourced from BuildConfig/config, default 60_000ms.
     */
    fun tick(packageName: String, intervalMs: Long): Boolean {
        val now = SystemClock.elapsedRealtime()
        val current = _snapshot.value

        return when (current.state) {
            State.IDLE -> {
                // Start new window
                windowStartElapsedMs = now
                _snapshot.value = current.copy(
                    state = State.RUNNING,
                    activePackage = packageName,
                    elapsedInSessionMs = 0L,
                    sessionStartElapsedMs = now
                )
                Log.i(TAG, "IDLE → RUNNING for $packageName (interval=${intervalMs}ms)")
                false
            }

            State.RUNNING -> {
                if (current.activePackage != packageName) {
                    // Different monitored app came to foreground – reset for new app
                    windowStartElapsedMs = now
                    _snapshot.value = current.copy(
                        activePackage = packageName,
                        elapsedInSessionMs = 0L,
                        sessionStartElapsedMs = now
                    )
                    Log.i(TAG, "RUNNING: app switched ${current.activePackage} → $packageName, timer reset")
                    return false
                }

                val elapsed = now - windowStartElapsedMs
                _snapshot.value = current.copy(elapsedInSessionMs = elapsed)

                if (elapsed >= intervalMs) {
                    _snapshot.value = _snapshot.value.copy(state = State.BLOCKING)
                    Log.i(TAG, "RUNNING → BLOCKING for $packageName after ${elapsed}ms")
                    true
                } else {
                    false
                }
            }

            State.BLOCKING -> {
                // Already blocking, caller should not tick – but safe to ignore
                false
            }
        }
    }

    /**
     * Called when the foreground app is NOT a monitored app.
     * Pauses the running window but does NOT reset accumulation.
     * This means: if child alt-tabs to browser for 2s, those 2s don't count.
     * The window pauses and resumes when they return.
     *
     * For MVP: we actually RESET the window here because:
     * 1. It's simpler to reason about
     * 2. Child switching apps briefly shouldn't lose their earned time
     *
     * TODO v1: Implement pause/resume with configurable grace period
     */
    fun onNonMonitoredForeground() {
        val current = _snapshot.value
        if (current.state == State.RUNNING) {
            // Pause by transitioning to IDLE, preserving accumulated time
            // MVP: reset to avoid complexity of partial windows
            Log.d(TAG, "RUNNING → IDLE (excluded app in foreground, timer reset)")
            _snapshot.value = current.copy(
                state = State.IDLE,
                elapsedInSessionMs = 0L
            )
            windowStartElapsedMs = 0L
        }
        // If BLOCKING: do not reset – child must still see the block
    }

    /**
     * Called when child acknowledges the block (presses OK).
     * Resets to IDLE so a fresh 60s window begins.
     */
    fun onBlockAcknowledged() {
        Log.i(TAG, "BLOCKING → IDLE (block acknowledged)")
        windowStartElapsedMs = 0L
        _snapshot.value = TimerSnapshot(
            state = State.IDLE,
            activePackage = null,
            elapsedInSessionMs = 0L,
            sessionStartElapsedMs = 0L
        )
    }

    /**
     * Called on service restart to restore state.
     * If the service was killed mid-window, we conservatively reset.
     * This is safe: child loses at most 60s of "earned" time, which is acceptable.
     *
     * In v1: persist windowStartElapsedMs and restore it here.
     */
    fun reset() {
        windowStartElapsedMs = 0L
        _snapshot.value = TimerSnapshot(
            state = State.IDLE,
            activePackage = null,
            elapsedInSessionMs = 0L,
            sessionStartElapsedMs = 0L
        )
    }

    companion object {
        private const val TAG = "KG_Timer"
    }

    fun getCurrentState(): State = _snapshot.value.state

    fun getElapsedMs(): Long = _snapshot.value.elapsedInSessionMs
}
