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
 * killing its process, freezing the app) without telling the user. The
 * listener therefore scans the notifications still in the shade on every
 * (re)connect and whenever the app returns to the foreground, capturing the
 * recent ones it has not handled; handled versions are remembered in the
 * settings table, so the catch-up cannot duplicate a capture or re-add one the
 * user already confirmed. Notifications dismissed before recovery are
 * unrecoverable.
 */
class PaymentCaptureListener : NotificationListenerService(), CaptureRecoveryListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.isOngoing) return
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        scope.launch { serialized { processPostedWithRetry(sbn) } }
    }

    override fun onListenerConnected() {
        Log.i(TAG, "Listener connected")
        connected = this
        recoverActive()
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "Listener disconnected")
        if (connected === this) connected = null
        // Re-enables a listener the system snoozed; a listener the user
        // turned off stays off, because the system denies the request
        // without access.
        requestRebindSafely(this)
    }

    override fun onDestroy() {
        if (connected === this) connected = null
        scope.cancel()
        super.onDestroy()
    }

    override fun recoverMissed() {
        recoverActive()
    }

    /**
     * Captures the notifications still in the shade, on the listener's IO scope:
     * reading the shade is a binder call. The connect and resume paths use the
     * same scope so neither blocks the main thread; posted notifications and the
     * catch-up are serialized by [mutex] and deduped by fingerprint.
     */
    private fun recoverActive() {
        scope.launch {
            val active = snapshotActive()
            serialized { catchUp(active) }
        }
    }

    private fun snapshotActive(): List<StatusBarNotification> = try {
        activeNotifications?.toList().orEmpty()
    } catch (e: Exception) {
        Log.w(TAG, "Cannot read the active notifications", e)
        emptyList()
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
        retrying(label = "catch-up notifications") { ingest ->
            // Filter by package before reading any content, so a non-allowlisted
            // app's notification is never parsed.
            val allowed = ingest.allowedPackages()
            val candidates = active
                .filter { it.packageName in allowed }
                .mapNotNull { it.toCandidate() }
            val result = ingest.catchUp(candidates)
            result.seeded?.let { Log.i(TAG, "Seeded $it active notifications") }
        }
    }

    /**
     * Reads the allowlist before the notification content: only an allowlisted
     * package is turned into a [CaptureCandidate]. Uses the same retry as the
     * catch-up, so one bad connection cannot drop the notification.
     */
    private suspend fun processPostedWithRetry(sbn: StatusBarNotification) {
        retrying(label = sbn.packageName) { ingest ->
            if (!ingest.isAllowed(sbn.packageName)) return@retrying
            val candidate = sbn.toCandidate() ?: return@retrying
            ingest.process(candidate)
        }
    }

    /**
     * Runs [block] against a repository, retrying a failed database write with a
     * fresh connection: a connection left in a bad state would otherwise make
     * every later capture fail until the process restarts.
     */
    private suspend fun retrying(label: String, block: suspend (CaptureIngest) -> Unit) {
        var lastError: Exception? = null
        for (attempt in 0 until PROCESS_ATTEMPTS) {
            if (attempt > 0) delay(RETRY_DELAY)
            try {
                val repository = sharedRepository(applicationContext)
                if (repository == null) {
                    lastError = IllegalStateException("Cannot open the database")
                } else {
                    block(CaptureIngest(repository))
                    return
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                discardSharedRepository()
            }
        }
        Log.w(TAG, "Failed to process $label", lastError)
    }

    private fun StatusBarNotification.toCandidate(): CaptureCandidate? = try {
        val notification = notification ?: return null
        if (isOngoing) return null
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        // Extras is a public Java field that can come back null after parcelling.
        val extras = notification.extras ?: return null
        CaptureCandidate(
            packageName = packageName,
            key = key,
            postTime = postTime,
            content = CaptureNotification(
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
                subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            ),
        )
    } catch (e: Exception) {
        // A third-party notification with malformed extras must not crash the listener.
        Log.w(TAG, "Cannot read a notification", e)
        null
    }

    companion object {
        private const val TAG = "PaymentCaptureListener"
        private const val PROCESS_ATTEMPTS = 3
        private val RETRY_DELAY = 500.milliseconds

        @Volatile
        private var connected: PaymentCaptureListener? = null

        @Volatile
        private var repository: CaptureRepository? = null

        @Volatile
        private var driver: SqlDriver? = null

        private val lock = Any()

        /**
         * Called when the app returns to the foreground. The system can stop
         * delivering callbacks or unbind the listener without telling the app;
         * asking for a rebind and re-scanning the shade recovers what is still
         * there instead of waiting for the next process restart.
         */
        fun recover(context: Context) {
            if (connected == null) Log.i(TAG, "No connected listener; requesting a rebind")
            CaptureRecovery(
                listener = { connected },
                requestRebind = { requestRebindSafely(context) },
            ).recover()
        }

        private fun requestRebindSafely(context: Context) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(context, PaymentCaptureListener::class.java),
                )
            } catch (e: Exception) {
                Log.w(TAG, "Cannot request a listener rebind", e)
            }
        }

        /**
         * One database connection for the process, shared by every bind of the
         * listener and never closed while healthy — the app's own connection
         * lives just as long. Null when the database cannot be opened; then
         * nothing is captured rather than crashing the listener.
         */
        private suspend fun sharedRepository(context: Context): CaptureRepository? {
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
        private fun discardSharedRepository() {
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
