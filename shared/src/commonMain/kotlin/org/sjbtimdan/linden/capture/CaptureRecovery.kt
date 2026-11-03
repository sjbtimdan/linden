package org.sjbtimdan.linden.capture

/** A connected capture listener that can re-scan the notifications still in the shade. */
fun interface CaptureRecoveryListener {
    fun recoverMissed()
}

/**
 * Asks the system to rebind the notification listener and, when one is
 * connected, re-scans the notifications still in the shade. Extracted from the
 * Android service so the recovery decision can be unit-tested.
 */
class CaptureRecovery(
    private val listener: () -> CaptureRecoveryListener?,
    private val requestRebind: () -> Unit,
) {
    fun recover() {
        requestRebind()
        listener()?.recoverMissed()
    }
}
