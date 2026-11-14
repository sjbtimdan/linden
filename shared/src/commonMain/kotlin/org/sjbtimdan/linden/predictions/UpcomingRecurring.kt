package org.sjbtimdan.linden.predictions

import androidx.compose.runtime.Immutable
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** How many days ahead the upcoming-recurring section looks for bills. */
const val UPCOMING_HORIZON_DAYS = 7

/** How many upcoming recurring bills to show. */
const val UPCOMING_TOP_N = 5

/**
 * Feature toggle for the "Upcoming" recurring-bills banner in the entry form.
 * Temporarily disabled (2026-11-10): the heuristic does not reliably detect
 * upcoming payments, and dismissals are session-scoped rather than persisted.
 * Flip back to `true` to reinstate the banner — the section, dismissal plumbing
 * and tests are all still in place. See living-notes "Upcoming-payment dismiss
 * is session-scoped".
 */
val UPCOMING_ENABLED = false

/** A recurring bill whose next occurrence is due soon, with its detected cadence. */
@Immutable
data class UpcomingRecurring(
    val entry: Entry,
    val cadence: RecurrenceCadence,
    val nextDue: Instant,
)

/**
 * Returns recurring bills of [type] whose next occurrence — the most recent
 * entry plus the detected cadence's interval — lands within the due window,
 * most imminent first. Monthly bills surface up to [UPCOMING_HORIZON_DAYS] ahead, so a
 * bill a few days out still reads as a heads-up; weekly bills only while
 * actually due (within [DUE_WINDOW_DAYS]), since their next occurrence is
 * always inside a week and would otherwise never leave the list. A description
 * must recur; its most recent occurrence is the representative entry, whose
 * amount is the best guess for the next bill. Descriptions with no detected
 * cadence are ignored.
 */
fun predictUpcomingRecurring(
    entries: List<Entry>,
    type: EntryType,
    now: Instant,
    timeZone: TimeZone,
    suppressed: Set<String> = emptySet(),
): List<UpcomingRecurring> {
    val typed = entries.filter { it.type == type && !it.description.isNullOrBlank() }
    return typed
        .groupBy { it.description!!.lowercase() }
        .mapNotNull { (description, group) ->
            if (description in suppressed) return@mapNotNull null
            val cadence = recurringCadence(group, description) ?: return@mapNotNull null
            val last = group.maxBy { it.createdAt }
            val nextDue = nextOccurrence(last.createdAt, cadence, timeZone)
            val forward = when (cadence) {
                RecurrenceCadence.Weekly -> DUE_WINDOW_DAYS.days
                RecurrenceCadence.Monthly -> UPCOMING_HORIZON_DAYS.days
            }
            if (nextDue < now - DUE_WINDOW_DAYS.days || nextDue > now + forward) {
                return@mapNotNull null
            }
            UpcomingRecurring(last, cadence, nextDue)
        }
        .sortedBy { it.nextDue }
        .take(UPCOMING_TOP_N)
}
