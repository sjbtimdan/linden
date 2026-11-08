package org.sjbtimdan.linden.predictions

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Entry
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

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
 * (~7 days) or monthly (~30 days) interval. At least [MIN_RECURRING_OCCURRENCES]
 * entries are required so a one-off pair is never flagged as a subscription, and at
 * least [RECURRING_MAJORITY_FRACTION] of the gaps must fit the cadence so that
 * one-off outliers don't hide it. Returns null when no regular interval is
 * found.
 */
fun recurringCadence(entries: List<Entry>, description: String): RecurrenceCadence? {
    val matching = entries
        .filter { it.description?.equals(description, ignoreCase = true) == true }
        .sortedBy { it.createdAt }
    if (matching.size < MIN_RECURRING_OCCURRENCES) return null
    val gaps = matching.zipWithNext { a, b -> (b.createdAt - a.createdAt).inWholeDays }
    return when {
        gaps.matchesCadence(WEEKLY_DAYS, WEEKLY_TOLERANCE_DAYS) -> RecurrenceCadence.Weekly
        gaps.matchesCadence(MONTHLY_DAYS, MONTHLY_TOLERANCE_DAYS) -> RecurrenceCadence.Monthly
        else -> null
    }
}

/**
 * The next occurrence of a recurring entry after [last], in [zone]: a week for
 * weekly entries, the next calendar month for monthly ones. The day-of-month is
 * preserved — clamped to the target month's last day, so the 31st follows onto
 * the 28th/29th — keeping a 1st-of-month bill on the 1st instead of drifting
 * with a fixed 30-day interval.
 */
fun nextOccurrence(last: Instant, cadence: RecurrenceCadence, zone: TimeZone): Instant = when (cadence) {
    RecurrenceCadence.Weekly -> last + WEEKLY_DAYS.days

    RecurrenceCadence.Monthly -> {
        val local = last.toLocalDateTime(zone)
        LocalDateTime(local.date.plus(1, DateTimeUnit.MONTH), local.time).toInstant(zone)
    }
}

/** Whether enough gaps sit within [toleranceDays] of [intervalDays]. */
private fun List<Long>.matchesCadence(intervalDays: Int, toleranceDays: Int): Boolean {
    val required = maxOf(MIN_RECURRING_MATCHES, ceil(size * RECURRING_MAJORITY_FRACTION).toInt())
    return count { abs(it - intervalDays) <= toleranceDays } >= required
}
