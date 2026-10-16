package org.sjbtimdan.linden.capture

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.sjbtimdan.linden.data.DatabaseDriverFactory
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.createLindenDatabase

/**
 * Reads notifications from user-allowlisted apps and turns payment-like ones
 * into pending captures. The allowlist is checked before any content is read;
 * everything else returns untouched, and nothing reaches the ledger from here
 * — the user reviews and confirms each capture in the app.
 *
 * Declared in the manifest with BIND_NOTIFICATION_LISTENER_SERVICE and
 * `android:exported="false"`: only the system binds it, and only once the user
 * granted notification access.
 */
class PaymentCaptureListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    /** Content hash last parsed per notification key, so updates don't re-capture. */
    private val processed = mutableMapOf<String, Int>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        if (sbn.isOngoing) return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        scope.launch {
            mutex.withLock { process(sbn.packageName, sbn.key, sbn.postTime, notification) }
        }
    }

    override fun onListenerDisconnected() {
        processed.clear()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun process(packageName: String, key: String, postTime: Long, notification: Notification) {
        val repository = sharedRepository(applicationContext) ?: return
        if (repository.rules().none { it.packageName == packageName && it.enabled }) return

        val content = CaptureNotification(
            title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            bigText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            subText = notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
        )
        if (processed.size > PROCESSED_LIMIT) processed.clear()
        if (processed[key] == content.hashCode()) return
        processed[key] = content.hashCode()

        val parsed = parseCaptureNotification(content) ?: return
        repository.addPending(
            PendingCapture(
                packageName = packageName,
                amountMinor = parsed.amountMinor,
                currency = parsed.currency,
                description = parsed.description,
                type = parsed.type,
                capturedAtMillis = postTime,
            ),
        )
    }

    private companion object {
        const val PROCESSED_LIMIT = 200

        /**
         * One database connection for the process, shared by every bind of the
         * listener and never closed — the app's own connection lives just as
         * long. Null when the database cannot be opened; then nothing is
         * captured rather than crashing the listener.
         */
        @Volatile
        private var repository: CaptureRepository? = null

        private val lock = Any()

        suspend fun sharedRepository(context: Context): CaptureRepository? {
            repository?.let { return it }
            val created = try {
                val settingsDao = SettingsDao(
                    createLindenDatabase(DatabaseDriverFactory(context).createDriver()).settingsQueries,
                )
                CaptureRepository(settingsDao)
            } catch (_: Exception) {
                return null
            }
            synchronized(lock) {
                repository?.let { return it }
                repository = created
            }
            return created
        }
    }
}
