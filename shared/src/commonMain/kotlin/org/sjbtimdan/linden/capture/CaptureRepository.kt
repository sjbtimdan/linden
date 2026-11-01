package org.sjbtimdan.linden.capture

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.sjbtimdan.linden.data.CAPTURE_PROCESSED_KEY
import org.sjbtimdan.linden.data.CAPTURE_RULES_KEY
import org.sjbtimdan.linden.data.CAPTURE_SEEN_KEY
import org.sjbtimdan.linden.data.CAPTURE_TEMPLATES_KEY
import org.sjbtimdan.linden.data.PENDING_CAPTURES_KEY
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Entry

/** How many pending captures are kept; the oldest ones fall off the tail. */
const val MAX_PENDING_CAPTURES = 50

/** How many learned templates are kept; the oldest learned falls off the tail. */
const val MAX_CAPTURE_TEMPLATES = 100

/** How many handled-notification fingerprints are remembered; the oldest falls off the tail. */
const val MAX_PROCESSED_NOTIFICATIONS = 200

/**
 * Reads and writes the capture allowlist, the pending-capture inbox and the
 * learned templates as JSON values in the settings table, so the feature needs
 * no schema change and rides along in backups. A corrupt value reads as empty
 * rather than crashing the app or the notification listener.
 */
class CaptureRepository(
    private val settingsDao: SettingsDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    companion object {
        private val _inboxChanges = MutableStateFlow(0L)

        /**
         * In-process ticks after every pending-inbox write. The notification
         * listener opens its own database connection, which [pendingFlow]
         * cannot observe; the review UI watches this to refresh at once.
         */
        val inboxChanges: StateFlow<Long> = _inboxChanges.asStateFlow()

        private val rulesSerializer = ListSerializer(CaptureRule.serializer())
        private val pendingSerializer = ListSerializer(PendingCapture.serializer())
        private val templatesSerializer = ListSerializer(CaptureTemplate.serializer())
        private val processedSerializer = ListSerializer(String.serializer())

        private fun notifyInboxChanged() {
            _inboxChanges.update { it + 1 }
        }
    }

    suspend fun rules(): List<CaptureRule> = readList(CAPTURE_RULES_KEY, rulesSerializer)

    fun rulesFlow(): Flow<List<CaptureRule>> = listFlow(CAPTURE_RULES_KEY, rulesSerializer)

    /** Adds [rule] or replaces the existing rule for its package. */
    suspend fun setRule(rule: CaptureRule) {
        writeList(
            CAPTURE_RULES_KEY,
            rulesSerializer,
            rules().filterNot { it.packageName == rule.packageName } + rule,
        )
    }

    suspend fun removeRule(packageName: String) {
        writeList(CAPTURE_RULES_KEY, rulesSerializer, rules().filterNot { it.packageName == packageName })
    }

    suspend fun setRuleEnabled(packageName: String, enabled: Boolean) {
        writeList(
            CAPTURE_RULES_KEY,
            rulesSerializer,
            rules().map { if (it.packageName == packageName) it.copy(enabled = enabled) else it },
        )
    }

    suspend fun pending(): List<PendingCapture> = readList(PENDING_CAPTURES_KEY, pendingSerializer)

    fun pendingFlow(): Flow<List<PendingCapture>> = listFlow(PENDING_CAPTURES_KEY, pendingSerializer)

    suspend fun templates(): List<CaptureTemplate> = readList(CAPTURE_TEMPLATES_KEY, templatesSerializer)

    fun templatesFlow(): Flow<List<CaptureTemplate>> = listFlow(CAPTURE_TEMPLATES_KEY, templatesSerializer)

    /**
     * Fingerprints of notification versions already handled, newest first, or
     * null before the listener has ever seeded the store. The listener reads
     * the active notifications again after a reconnect and skips these, so
     * catching up cannot duplicate a capture or re-add one already confirmed.
     */
    suspend fun processedNotifications(): List<String>? =
        settingsDao.getRaw(CAPTURE_PROCESSED_KEY)?.let { decodeList(it, processedSerializer) }

    /** Remembers [fingerprints] as handled, newest first and capped; an empty batch still seeds the store. */
    suspend fun rememberProcessed(fingerprints: Collection<String>) {
        val current = processedNotifications().orEmpty()
        writeList(
            CAPTURE_PROCESSED_KEY,
            processedSerializer,
            (fingerprints + current).distinct(),
            MAX_PROCESSED_NOTIFICATIONS,
        )
    }

    /**
     * Completes [captureId] once its entry is saved: remembers the template
     * [entry] teaches and removes the capture in one step. A capture that is
     * already gone has nothing to learn from and is simply left alone.
     */
    suspend fun completeCapture(captureId: Long, entry: Entry) {
        pending().firstOrNull { it.id == captureId }?.let { capture ->
            captureTemplateFor(capture, entry)?.let { template ->
                writeList(
                    CAPTURE_TEMPLATES_KEY,
                    templatesSerializer,
                    upsertTemplate(template),
                    MAX_CAPTURE_TEMPLATES,
                )
            }
        }
        removePending(captureId)
    }

    suspend fun removeTemplate(packageName: String, skeleton: String) {
        writeList(
            CAPTURE_TEMPLATES_KEY,
            templatesSerializer,
            templates().filterNot { it.packageName == packageName && it.skeleton == skeleton },
        )
    }

    /** The newest capture id the user has acknowledged; 0 before any dismissal. */
    suspend fun seenCaptureId(): Long = settingsDao.getRaw(CAPTURE_SEEN_KEY)?.toLongOrNull() ?: 0L

    fun seenCaptureIdFlow(): Flow<Long> = settingsDao.rawFlow(CAPTURE_SEEN_KEY).map { it?.toLongOrNull() ?: 0L }

    /** Moves the acknowledged watermark forward; it never regresses. */
    suspend fun markSeen(id: Long) {
        if (id > seenCaptureId()) settingsDao.setRaw(CAPTURE_SEEN_KEY, id.toString())
    }

    /**
     * Inserts [capture] as the newest entry and returns it with its assigned id.
     * The id floor includes the acknowledged watermark, so a capture inserted
     * after the inbox was emptied still reads as newer than every seen one.
     */
    suspend fun addPending(capture: PendingCapture): PendingCapture {
        val current = pending()
        val id = maxOf(current.maxOfOrNull { it.id } ?: 0L, seenCaptureId()) + 1
        val withId = capture.copy(id = id)
        writeList(
            PENDING_CAPTURES_KEY,
            pendingSerializer,
            listOf(withId) + current,
            MAX_PENDING_CAPTURES,
            onWrite = ::notifyInboxChanged,
        )
        return withId
    }

    /**
     * True when [capture] is already waiting in the inbox. Used before a retry
     * so a write that failed after the insert cannot add the capture twice; a
     * confirmed capture has left the inbox and is guarded by its fingerprint.
     */
    suspend fun containsPending(capture: PendingCapture): Boolean = pending().any {
        it.packageName == capture.packageName &&
            it.capturedAtMillis == capture.capturedAtMillis &&
            it.amountMinor == capture.amountMinor &&
            it.description == capture.description
    }

    suspend fun removePending(id: Long) {
        writeList(
            PENDING_CAPTURES_KEY,
            pendingSerializer,
            pending().filterNot { it.id == id },
            onWrite = ::notifyInboxChanged,
        )
    }

    suspend fun clearPending() {
        writeList(
            PENDING_CAPTURES_KEY,
            pendingSerializer,
            emptyList(),
            onWrite = ::notifyInboxChanged,
        )
    }

    /** [template] first, replacing the same package-and-skeleton key. */
    private suspend fun upsertTemplate(template: CaptureTemplate): List<CaptureTemplate> {
        val replaced = templates().filterNot {
            it.packageName == template.packageName && it.skeleton == template.skeleton
        }
        return listOf(template) + replaced
    }

    private suspend fun <T> readList(key: String, serializer: KSerializer<List<T>>): List<T> =
        decodeList(settingsDao.getRaw(key), serializer)

    private fun <T> listFlow(key: String, serializer: KSerializer<List<T>>): Flow<List<T>> =
        settingsDao.rawFlow(key).map { decodeList(it, serializer) }

    private suspend fun <T> writeList(
        key: String,
        serializer: KSerializer<List<T>>,
        values: List<T>,
        max: Int? = null,
        onWrite: () -> Unit = {},
    ) {
        settingsDao.setRaw(key, json.encodeToString(serializer, max?.let { values.take(it) } ?: values))
        onWrite()
    }

    private fun <T> decodeList(raw: String?, serializer: KSerializer<List<T>>): List<T> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            json.decodeFromString(serializer, raw)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }
}
