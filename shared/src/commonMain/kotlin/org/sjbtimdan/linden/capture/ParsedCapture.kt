package org.sjbtimdan.linden.capture

import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType

/**
 * A payment recognized in a notification, ready for the confirm-first inbox.
 * [amountMinor] is always positive — [type] carries the direction — and stored
 * at the fixed 2-decimal scale every Linden currency uses. [currency] is null
 * when the text only carried an ambiguous symbol ("$", "¥"): the currency of
 * the account the rule maps to applies then.
 */
data class ParsedCapture(
    val amountMinor: Long,
    val currency: Currency?,
    val description: String,
    val type: EntryType,
)
