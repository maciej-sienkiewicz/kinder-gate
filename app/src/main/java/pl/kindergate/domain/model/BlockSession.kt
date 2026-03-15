package pl.kindergate.domain.model

/**
 * Records a single PAUSE event shown to the child.
 *
 * elapsedRealtimeTriggeredMs: monotonic clock at trigger (immune to time manipulation).
 * wallClockTriggeredMs: wall clock time for display purposes only, not for logic.
 * acknowledgedAtElapsedMs: when the child pressed OK; null if still showing.
 */
data class BlockSession(
    val id: Long = 0,
    val packageName: String,
    val elapsedRealtimeTriggeredMs: Long,
    val wallClockTriggeredMs: Long,
    val acknowledgedAtElapsedMs: Long? = null,
    val sessionDurationMs: Long = 0 // time child spent in monitored app before block
) {
    val isAcknowledged: Boolean get() = acknowledgedAtElapsedMs != null
}
