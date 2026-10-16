package org.sjbtimdan.linden.ui.capture

import org.sjbtimdan.linden.capture.PendingCapture

/**
 * One inbox row: the capture plus everything the list needs to render it.
 * [accountName] is null when the rule's account no longer exists or is hidden.
 */
data class CapturedPaymentItem(
    val capture: PendingCapture,
    val amountText: String,
    val accountName: String?,
    val accountId: Long?,
)
