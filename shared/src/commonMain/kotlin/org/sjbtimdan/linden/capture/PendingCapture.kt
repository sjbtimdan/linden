package org.sjbtimdan.linden.capture

import kotlinx.serialization.Serializable
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType

/**
 * A parsed notification waiting for the user's confirmation; it becomes an
 * entry only through the normal entry form, never automatically. Ids are
 * assigned by [CaptureRepository] at insert time (0 means "not inserted yet"),
 * captures are stored newest first and capped.
 */
@Serializable
data class PendingCapture(
    val id: Long = 0,
    val packageName: String,
    val amountMinor: Long,
    val currency: Currency? = null,
    val description: String = "",
    val type: EntryType,
    val capturedAtMillis: Long,
)
