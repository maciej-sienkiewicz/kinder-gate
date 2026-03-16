package pl.kindergate.feature.blocking

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import pl.kindergate.service.MonitorService
import pl.kindergate.ui.theme.KinderGateTheme

/**
 * Fullscreen blocking activity shown to the child after 60s of monitored app usage.
 *
 * Window flags design:
 * - FLAG_SHOW_WHEN_LOCKED: activity shows above keyguard/lock screen
 * - FLAG_KEEP_SCREEN_ON: prevents screen from sleeping during block
 * - FLAG_TURN_SCREEN_ON: wakes screen if it's off when block triggers
 * - FLAG_DISMISS_KEYGUARD: NOT set – we don't unlock the device,
 *   child still needs to unlock after seeing the pause screen
 *
 * Launch mode: singleInstance in a separate task (taskAffinity in manifest).
 * This ensures:
 * - Only one instance exists at a time
 * - It appears in recent apps as a separate task (but excludeFromRecents hides it)
 * - Home button brings it back if child navigates away
 *
 * Process: When child presses OK, we send ACTION_BLOCK_ACKNOWLEDGED to MonitorService
 * and finish this activity. The service resets the timer.
 *
 * Edge case – child presses back/home:
 * - We override onUserLeaveHint and onPause to handle this case
 * - If child navigates away, MonitorService's tick loop detects the monitored
 *   app returned to foreground and re-launches this activity
 *
 * Edge case – child uses recent apps to clear:
 * - excludeFromRecents = true prevents the blocking task from appearing
 *   in recent apps list
 *
 * Edge case – multiple rapid app switches:
 * - singleInstance + singleTask prevents multiple BlockingActivity instances
 */
@AndroidEntryPoint
class BlockingActivity : ComponentActivity() {

    private var packageName_: String = ""
    private var sessionId: Long = -1L
    private var isAcknowledged: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure window to show above lock screen and keep screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // API 27+ recommended way
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        packageName_ = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        isBlockingActiveGlobal = true

        setContent {
            KinderGateTheme {
                BlockingScreen(
                    onAcknowledge = { onChildAcknowledged() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        packageName_ = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: packageName_
        sessionId = intent.getLongExtra(EXTRA_SESSION_ID, sessionId)
    }

    private fun onChildAcknowledged() {
        isAcknowledged = true
        isBlockingActiveGlobal = false
        // Notify service BEFORE finishing so timer resets before we animate out
        startService(MonitorService.blockAcknowledgedIntent(this))
        finish()
    }

    /**
     * When the child navigates away (Home button, Recent Apps), immediately
     * bring ourselves back to front. This is Layer 2 of bypass prevention:
     * - Layer 1: MonitorService.tick() re-launches every 1s
     * - Layer 2: This onStop() gives near-instant response
     * - Layer 3: AccessibilityService detects window change immediately
     *
     * The isAcknowledged guard prevents re-launch when child presses OK.
     */
    override fun onStop() {
        super.onStop()
        if (!isAcknowledged && !isFinishing) {
            Log.i(TAG, "onStop: child navigated away, bringing blocking screen back to front")
            bringToFront()
        }
    }

    private fun bringToFront() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
    }

    // Do not allow back press to dismiss the blocking screen
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Intentionally swallowed – child cannot dismiss with back button
    }

    companion object {
        private const val TAG = "KG_Blocking"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_SESSION_ID = "extra_session_id"

        /**
         * Static flag read by KinderGateAccessibilityService to know whether
         * blocking is active. When true, the accessibility service will
         * immediately re-launch BlockingActivity on any window change to
         * a non-KinderGate app – providing near-zero-latency bypass prevention.
         */
        @Volatile
        @JvmStatic
        var isBlockingActiveGlobal: Boolean = false
            private set
    }
}
