package org.sjbtimdan.linden.capture

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.sjbtimdan.linden.data.DatabaseDriverFactory
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.createLindenDatabase
import org.sjbtimdan.linden.time.SystemClock
import java.util.TimeZone
import kotlin.time.Duration.Companion.milliseconds

/**
 * Reads notifications from user-allowlisted apps and turns payment-like ones
 * into pending captures. The allowlist is checked before any content is read;
 * everything else returns untouched, and nothing reaches the ledger from here
 * — the user reviews and confirms each capture in the app.
 *
 * Declared in the manifest with BIND_NOTIFICATION_LISTENER_SERVICE and
 * `android:exported="false"`: only the system binds it, and only once the user
 * granted notification access.
 *
 * The system can stop delivering callbacks for hours (rebinding the service,
 * killing its process, freezing the app) without telling the user. On every
 * (re)connect the listener therefore scans the notifications still in the
 * shade and captures the recent ones it has not handled; handled versions are
 * remembered in the settings table, so the catch-up cannot duplicate a capture
 * or re-add one the user already confirmed. Notifications dismissed before the
 * reconnect are unrecoverable.
 */
class PaymentCaptureListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val candidate = sbn.toCandidate() ?: return
        scope.launch { serialized { processWithRetry(candidate) } }
    }

    override fun onListenerConnected() {
        Log.i(TAG, "Listener connected")
        // Snapshot synchronously: a notification posted after this point is
        // delivered to onNotificationPosted, and processing it before the
        // catch-up seeds the store must not make it look already handled.
        val active = try {
            activeNotifications?.toList().orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read the active notifications", e)
            emptyList()
        }
        scope.launch { serialized { catchUp(active) } }
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "Listener disconnected")
        try {
            // Re-enables a listener the system snoozed; a listener the user
            // turned off stays off, because the system denies the request
            // without access.
            requestRebind(ComponentName(this, PaymentCaptureListener::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "Cannot request a listener rebind", e)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun serialized(block: suspend () -> Unit) {
        try {
            mutex.withLock { block() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Capture work failed", e)
        }
    }

    /** Captures the recent notifications still in the shade that this listener has not handled. */
    private suspend fun catchUp(active: List<StatusBarNotification>) {
        val repository = sharedRepository(applicationContext) ?: return
        val allowlisted = repository.rules().filter { it.enabled }.mapTo(HashSet()) { it.packageName }
        val cutoff = SystemClock.now().toEpochMilliseconds() - CATCH_UP_WINDOW_MILLIS
        val candidates = active.mapNotNull { it.toCandidate() }
            .filter { it.packageName in allowlisted && it.postTime >= cutoff }
            .sortedBy { it.postTime }
        val processed = repository.processedNotifications()
        if (processed == null) {
            // First connect under this version: remember what is already in
            // the shade instead of importing it all as new captures.
            repository.rememberProcessed(candidates.map(::fingerprint))
            Log.i(TAG, "Seeded ${candidates.size} active notifications")
            return
        }
        candidates.filterNot { fingerprint(it) in processed }.forEach { processWithRetry(it) }
    }

    /**
     * Processes [candidate], retrying a failed database write with a fresh
     * connection: a connection left in a bad state would otherwise make every
     * later capture fail until the process restarts.
     */
    private suspend fun processWithRetry(candidate: Candidate) {
        var lastError: Exception? = null
        for (attempt in 0 until PROCESS_ATTEMPTS) {
            if (attempt > 0) delay(RETRY_DELAY)
            try {
                val repository = sharedRepository(applicationContext)
                if (repository == null) {
                    lastError = IllegalStateException("Cannot open the database")
                } else {
                    process(candidate, repository)
                    return
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                discardSharedRepository()
            }
        }
        Log.w(TAG, "Failed to process a notification from ${candidate.packageName}", lastError)
    }

    private suspend fun process(candidate: Candidate, repository: CaptureRepository) {
        if (repository.rules().none { it.enabled && it.packageName == candidate.packageName }) return
        val fingerprint = fingerprint(candidate)
        if (repository.processedNotifications()?.contains(fingerprint) == true) return

        val parsed = parseCaptureNotification(candidate.content) ?: return
        val capture = PendingCapture(
            packageName = candidate.packageName,
            amountMinor = parsed.amountMinor,
            currency = parsed.currency,
            description = parsed.description,
            type = parsed.type,
            capturedAtMillis = candidate.postTime,
            // The device zone when the notification arrived, so an entry
            // confirmed after a flight still lands on the payment's day.
            capturedAtZoneId = TimeZone.getDefault().id,
        )
        if (!repository.containsPending(capture)) repository.addPending(capture)
        repository.rememberProcessed(listOf(fingerprint))
    }

    private fun fingerprint(candidate: Candidate): String = captureFingerprint(candidate.key, candidate.content)

    private data class Candidate(
        val packageName: String,
        val key: String,
        val postTime: Long,
        val content: CaptureNotification,
    )

    private fun StatusBarNotification.toCandidate(): Candidate? {
        val notification = notification ?: return null
        if (isOngoing) return null
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        return Candidate(
            packageName = packageName,
            key = key,
            postTime = postTime,
            content = CaptureNotification(
                title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                bigText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
                subText = notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            ),
        )
    }

    private companion object {
        const val TAG = "PaymentCaptureListener"
        const val CATCH_UP_WINDOW_MILLIS = 24 * 60 * 60 * 1000L
        const val PROCESS_ATTEMPTS = 3
        val RETRY_DELAY = 500.milliseconds

        @Volatile
        private var repository: CaptureRepository? = null

        @Volatile
        private var driver: SqlDriver? = null

        private val lock = Any()

        /**
         * One database connection for the process, shared by every bind of the
         * listener and never closed while healthy — the app's own connection
         * lives just as long. Null when the database cannot be opened; then
         * nothing is captured rather than crashing the listener.
         */
        suspend fun sharedRepository(context: Context): CaptureRepository? {
            repository?.let { return it }
            val created = try {
                val newDriver = DatabaseDriverFactory(context).createDriver()
                val database = createLindenDatabase(newDriver)
                CaptureRepository(SettingsDao(database.settingsQueries)) to newDriver
            } catch (_: Exception) {
                return null
            }
            synchronized(lock) {
                repository?.let {
                    created.second.close()
                    return it
                }
                driver = created.second
                repository = created.first
            }
            return created.first
        }

        /** Drops the cached connection so the next attempt opens a fresh one. */
        fun discardSharedRepository() {
            synchronized(lock) {
                repository = null
                try {
                    driver?.close()
                } catch (_: Exception) {
                    // A connection that cannot close is already unusable.
                }
                driver = null
            }
        }
    }
}
