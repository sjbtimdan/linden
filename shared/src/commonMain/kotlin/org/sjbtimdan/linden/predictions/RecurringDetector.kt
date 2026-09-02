package org.sjbtimdan.linden.predictions

import org.sjbtimdan.linden.model.Entry
import kotlin.math.abs

/** How regularly a set of entries recurs. */
enum class RecurrenceCadence { Weekly, Monthly }

internal const val WEEKLY_DAYS = 7
internal const val MONTHLY_DAYS = 30
internal const val WEEKLY_TOLERANCE_DAYS = 2
internal const val MONTHLY_TOLERANCE_DAYS = 5
internal const val MIN_RECURRING_OCCURRENCES = 3

/**
 * Detects whether entries matching [description] recur at a regular cadence.
 *
 * Entries are matched by description (case-insensitive), sorted by date, and
 * the gaps between consecutive occurrences are checked for a consistent weekly
 * (~7 days) or monthly (~30 days) interval. At least [minOccurrences] entries
 * are required so a one-off pair is never flagged as a subscription. Returns
 * null when no regular interval is found.
 */
fun recurringCadence(
    entries: List<Entry>,
    description: String,
    minOccurrences: Int = MIN_RECURRING_OCCURRENCES,
): RecurrenceCadence? {
    val matching = entries
        .filter { it.description?.equals(description, ignoreCase = true) == true }
        .sortedBy { it.createdAt }
    if (matching.size < minOccurrences) return null
    val gaps = matching.zipWithNext { a, b -> (b.createdAt - a.createdAt).inWholeDays }
    return when {
        gaps.all { abs(it - WEEKLY_DAYS) <= WEEKLY_TOLERANCE_DAYS } -> RecurrenceCadence.Weekly
        gaps.all { abs(it - MONTHLY_DAYS) <= MONTHLY_TOLERANCE_DAYS } -> RecurrenceCadence.Monthly
        else -> null
    }
}
