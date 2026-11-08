package org.sjbtimdan.linden.predictions

import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** How many days ahead the upcoming-recurring section looks for bills. */
const val UPCOMING_HORIZON_DAYS = 7

/** How many upcoming recurring bills to show. */
const val UPCOMING_TOP_N = 5

/** A recurring bill whose next occurrence is due soon, with its detected cadence. */
data class UpcomingRecurring(
    val entry: Entry,
    val cadence: RecurrenceCadence,
    val nextDue: Instant,
)

/**
 * Returns recurring bills of [type] whose next occurrence — the most recent
 * entry plus the detected cadence's interval — lands within [now]'s overdue
 * allowance ([DUE_WINDOW_DAYS] behind) through [horizonDays] ahead, most
 * imminent first. A description must recur (weekly or monthly); its most recent
 * occurrence is the representative entry, whose amount is the best guess for
 * the next bill. Descriptions with no detected cadence are ignored.
 */
fun predictUpcomingRecurring(
    entries: List<Entry>,
    type: EntryType,
    now: Instant,
    horizonDays: Int = UPCOMING_HORIZON_DAYS,
    topN: Int = UPCOMING_TOP_N,
): List<UpcomingRecurring> {
    val typed = entries.filter { it.type == type && !it.description.isNullOrBlank() }
    return typed
        .groupBy { it.description!!.lowercase() }
        .mapNotNull { (description, group) ->
            val cadence = recurringCadence(group, description) ?: return@mapNotNull null
            val last = group.maxBy { it.createdAt }
            val interval = when (cadence) {
                RecurrenceCadence.Weekly -> WEEKLY_DAYS.days
                RecurrenceCadence.Monthly -> MONTHLY_DAYS.days
            }
            val nextDue = last.createdAt + interval
            if (nextDue < now - DUE_WINDOW_DAYS.days || nextDue > now + horizonDays.days) {
                return@mapNotNull null
            }
            UpcomingRecurring(last, cadence, nextDue)
        }
        .sortedBy { it.nextDue }
        .take(topN)
}
