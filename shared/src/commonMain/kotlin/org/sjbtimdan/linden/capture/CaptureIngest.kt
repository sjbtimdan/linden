package org.sjbtimdan.linden.capture

import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.time.AppClock
import org.sjbtimdan.linden.time.SystemClock

/** How far back a reconnect may import notifications still in the shade. */
const val CATCH_UP_WINDOW_MILLIS: Long = 24 * 60 * 60 * 1000L

/**
 * One notification read from the shade before its content is parsed. Mirrors
 * the platform data so the ingest rules stay in common code.
 */
data class CaptureCandidate(
    val packageName: String,
    val key: String,
    val postTime: Long,
    val content: CaptureNotification,
)

/**
 * The allowlisted candidates posted no earlier than [windowMillis] before
 * [nowMillis], oldest first, so a catch-up imports in posting order and the
 * newest capture ends up at the top of the inbox.
 */
fun recentCaptureCandidates(
    candidates: List<CaptureCandidate>,
    allowedPackages: Set<String>,
    nowMillis: Long,
    windowMillis: Long = CATCH_UP_WINDOW_MILLIS,
): List<CaptureCandidate> = candidates
    .filter { it.packageName in allowedPackages && it.postTime >= nowMillis - windowMillis }
    .sortedBy { it.postTime }

/**
 * Outcome of a catch-up scan; [seeded] is non-null on the first connect after
 * install or update, when the shade is recorded as handled instead of imported.
 */
data class CaptureCatchUpResult(
    val imported: Int,
    val seeded: Int? = null,
)

/**
 * Turns allowlisted notifications into pending captures and remembers the
 * fingerprints of the versions already handled, so a reconnect or a re-post can
 * neither duplicate a capture nor re-add one the user confirmed. Kept away from
 * the Android service so its ordering and dedupe rules can be unit-tested
 * against a real database.
 */
class CaptureIngest(
    private val repository: CaptureRepository,
    private val clock: AppClock = SystemClock,
    private val zoneId: () -> String = { TimeZone.currentSystemDefault().id },
) {
    /** Whether [packageName] may have its notification content read at all. */
    suspend fun isAllowed(packageName: String): Boolean =
        repository.rules().any { it.enabled && it.packageName == packageName }

    /** Packages whose notification content may be read at all. */
    suspend fun allowedPackages(): Set<String> =
        repository.rules().filter { it.enabled }.mapTo(HashSet()) { it.packageName }

    /**
     * Handles [candidate]. Returns the pending capture it created, or null when
     * the app is not allowlisted, the notification is not a payment, or this
     * version was already handled.
     */
    suspend fun process(candidate: CaptureCandidate): PendingCapture? {
        if (!isAllowed(candidate.packageName)) return null
        val fingerprint = fingerprint(candidate)
        if (repository.processedNotifications()?.contains(fingerprint) == true) return null
        val parsed = parseCaptureNotification(candidate.content) ?: return null
        val capture = PendingCapture(
            packageName = candidate.packageName,
            amountMinor = parsed.amountMinor,
            currency = parsed.currency,
            description = parsed.description,
            type = parsed.type,
            capturedAtMillis = candidate.postTime,
            capturedAtZoneId = zoneId(),
        )
        if (!repository.containsPending(capture)) repository.addPending(capture)
        repository.rememberProcessed(listOf(fingerprint))
        return capture
    }

    /**
     * Imports the recent allowlisted candidates this listener has not handled.
     * On the first connect after install or update the current shade is recorded
     * as handled instead, so old notifications are not imported as new captures.
     */
    suspend fun catchUp(
        candidates: List<CaptureCandidate>,
        nowMillis: Long = clock.now().toEpochMilliseconds(),
    ): CaptureCatchUpResult {
        val recent = recentCaptureCandidates(candidates, allowedPackages(), nowMillis)
        val processed = repository.processedNotifications()
        if (processed == null) {
            repository.rememberProcessed(recent.map(::fingerprint))
            return CaptureCatchUpResult(imported = 0, seeded = recent.size)
        }
        var imported = 0
        for (candidate in recent) {
            if (fingerprint(candidate) in processed) continue
            if (process(candidate) != null) imported++
        }
        return CaptureCatchUpResult(imported = imported)
    }

    private fun fingerprint(candidate: CaptureCandidate): String = captureFingerprint(candidate.key, candidate.content)
}
