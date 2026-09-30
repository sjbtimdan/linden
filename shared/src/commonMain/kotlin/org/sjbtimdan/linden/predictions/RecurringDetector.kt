package org.sjbtimdan.linden.predictions

import org.sjbtimdan.linden.model.Entry
import kotlin.math.abs
import kotlin.math.ceil

/** How regularly a set of entries recurs. */
enum class RecurrenceCadence { Weekly, Monthly }

internal const val WEEKLY_DAYS = 7
internal const val MONTHLY_DAYS = 30
internal const val WEEKLY_TOLERANCE_DAYS = 2
internal const val MONTHLY_TOLERANCE_DAYS = 5
internal const val MIN_RECURRING_OCCURRENCES = 3

/**
 * Fraction of gaps that must sit within the cadence tolerance. A minority of
 * outliers is allowed, so a single mis-described entry (e.g. a small one-off
 * logged under a monthly bill's name) doesn't hide a real subscription.
 */
internal const val RECURRING_MAJORITY_FRACTION = 0.7

/** Gaps a series must match at minimum, so a 3-entry series must match entirely. */
internal const val MIN_RECURRING_MATCHES = 2

/**
 * Detects whether entries matching [description] recur at a regular cadence.
 *
 * Entries are matched by description (case-insensitive), sorted by date, and
 * the gaps between consecutive occurrences are checked for a consistent weekly
 * (~7 days) or monthly (~30 days) interval. At least [minOccurrences] entries
 * are required so a one-off pair is never flagged as a subscription, and at
 * least [RECURRING_MAJORITY_FRACTION] of the gaps must fit the cadence so that
 * one-off outliers don't hide it. Returns null when no regular interval is
 * found.
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
        gaps.matchesCadence(WEEKLY_DAYS, WEEKLY_TOLERANCE_DAYS) -> RecurrenceCadence.Weekly
        gaps.matchesCadence(MONTHLY_DAYS, MONTHLY_TOLERANCE_DAYS) -> RecurrenceCadence.Monthly
        else -> null
    }
}

/** Whether enough gaps sit within [toleranceDays] of [intervalDays]. */
private fun List<Long>.matchesCadence(intervalDays: Int, toleranceDays: Int): Boolean {
    val required = maxOf(MIN_RECURRING_MATCHES, ceil(size * RECURRING_MAJORITY_FRACTION).toInt())
    return count { abs(it - intervalDays) <= toleranceDays } >= required
}
