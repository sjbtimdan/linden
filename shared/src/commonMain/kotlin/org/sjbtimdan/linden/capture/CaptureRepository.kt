package org.sjbtimdan.linden.capture

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
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

        private fun notifyInboxChanged() {
            _inboxChanges.update { it + 1 }
        }
    }

    suspend fun rules(): List<CaptureRule> = decodeRules(settingsDao.getRaw(CAPTURE_RULES_KEY))

    fun rulesFlow(): Flow<List<CaptureRule>> = settingsDao.rawFlow(CAPTURE_RULES_KEY).map { decodeRules(it) }

    /** Adds [rule] or replaces the existing rule for its package. */
    suspend fun setRule(rule: CaptureRule) {
        writeRules(rules().filterNot { it.packageName == rule.packageName } + rule)
    }

    suspend fun removeRule(packageName: String) {
        writeRules(rules().filterNot { it.packageName == packageName })
    }

    suspend fun setRuleEnabled(packageName: String, enabled: Boolean) {
        writeRules(rules().map { if (it.packageName == packageName) it.copy(enabled = enabled) else it })
    }

    suspend fun pending(): List<PendingCapture> = decodePending(settingsDao.getRaw(PENDING_CAPTURES_KEY))

    fun pendingFlow(): Flow<List<PendingCapture>> = settingsDao.rawFlow(PENDING_CAPTURES_KEY).map { decodePending(it) }

    suspend fun templates(): List<CaptureTemplate> = decodeOrDefault(settingsDao.getRaw(CAPTURE_TEMPLATES_KEY))

    fun templatesFlow(): Flow<List<CaptureTemplate>> =
        settingsDao.rawFlow(CAPTURE_TEMPLATES_KEY).map { decodeOrDefault(it) }

    /**
     * Completes [captureId] once its entry is saved: remembers the template
     * [entry] teaches and removes the capture in one step. A capture that is
     * already gone has nothing to learn from and is simply left alone.
     */
    suspend fun completeCapture(captureId: Long, entry: Entry) {
        pending().firstOrNull { it.id == captureId }?.let { capture ->
            captureTemplateFor(capture, entry)?.let { writeTemplates(upsertTemplate(it)) }
        }
        removePending(captureId)
    }

    suspend fun removeTemplate(packageName: String, skeleton: String) {
        writeTemplates(templates().filterNot { it.packageName == packageName && it.skeleton == skeleton })
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
        writePending((listOf(withId) + current).take(MAX_PENDING_CAPTURES))
        return withId
    }

    suspend fun removePending(id: Long) {
        writePending(pending().filterNot { it.id == id })
    }

    suspend fun clearPending() {
        writePending(emptyList())
    }

    private fun decodeRules(raw: String?): List<CaptureRule> = decodeOrDefault(raw)

    private fun decodePending(raw: String?): List<PendingCapture> = decodeOrDefault(raw)

    private inline fun <reified T> decodeOrDefault(raw: String?): List<T> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            json.decodeFromString<List<T>>(raw)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    private suspend fun writeRules(rules: List<CaptureRule>) {
        settingsDao.setRaw(CAPTURE_RULES_KEY, json.encodeToString(rules))
    }

    private suspend fun writePending(pending: List<PendingCapture>) {
        settingsDao.setRaw(PENDING_CAPTURES_KEY, json.encodeToString(pending))
        notifyInboxChanged()
    }

    /** [template] first, replacing the same package-and-skeleton key and capped at [MAX_CAPTURE_TEMPLATES]. */
    private suspend fun upsertTemplate(template: CaptureTemplate): List<CaptureTemplate> {
        val replaced = templates().filterNot {
            it.packageName == template.packageName && it.skeleton == template.skeleton
        }
        return (listOf(template) + replaced).take(MAX_CAPTURE_TEMPLATES)
    }

    private suspend fun writeTemplates(templates: List<CaptureTemplate>) {
        settingsDao.setRaw(CAPTURE_TEMPLATES_KEY, json.encodeToString(templates))
    }
}
