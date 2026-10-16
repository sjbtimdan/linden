package org.sjbtimdan.linden.capture

import kotlinx.serialization.Serializable
import org.sjbtimdan.linden.model.EntryType

/**
 * A learned rule for one notification template: the app package and the
 * digit-masked skeleton of the description it was learned from, mapped to how
 * the user classified that capture. The fields prefill the entry form the next
 * time a notification of the same template arrives; nothing is ever written to
 * the ledger without the user's confirmation. Stored as JSON in the settings
 * table, so no schema change is needed and the rules ride along in backups.
 */
@Serializable
data class CaptureTemplate(
    val packageName: String,
    val skeleton: String,
    val type: EntryType,
    val categoryId: Long,
    val accountId: Long,
    /** The user's wording for the entry; null keeps the notification's own text. */
    val description: String? = null,
)
