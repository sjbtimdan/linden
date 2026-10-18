package org.sjbtimdan.linden.ui.ledger

import kotlin.math.roundToInt

/**
 * Signed percentage of [projected] against [typical], e.g. "+12%" or "−5%".
 * Null when there is no typical total to compare against.
 */
internal fun formatPaceDeltaPercent(projected: Long, typical: Long): String? {
    if (typical <= 0L) return null
    val percent = ((projected - typical).toDouble() / typical * 100).roundToInt()
    return when {
        percent > 0 -> "+$percent%"
        percent < 0 -> "−${-percent}%"
        else -> "0%"
    }
}
