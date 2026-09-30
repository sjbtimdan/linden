package org.sjbtimdan.linden.predictions

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Entry
import kotlin.math.abs
import kotlin.math.ln
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

const val QUICK_ENTRY_TOP_N = 5

/**
 * How many days around a recurring description's predicted next occurrence
 * still count as due, so a bill logged a day early or late still surfaces.
 */
internal const val DUE_WINDOW_DAYS = 2

/** A whole entry the user is likely to repeat right now, with its detected cadence. */
data class QuickEntry(
    val entry: Entry,
    val cadence: RecurrenceCadence?,
)

/**
 * Returns the whole entries an entry dated [target] is most likely to repeat.
 *
 * Descriptions whose detected cadence puts their next expected occurrence — the
 * most recent entry plus the weekly/monthly interval — within
 * [DUE_WINDOW_DAYS] of [target] float to the very top, above field matches, so
 * dating an entry to a recurring bill's usual day surfaces that bill. The
 * remaining candidates are ranked by time of day — hour, weekday, month and
 * day of month — against [target], the moment the new entry is dated,
 * multiplied by recency decay against [now] and a logarithmic frequency weight
 * so that recent, frequently-entered entries dominate. Entries that appear
 * only once are filtered out. Entries matching the draft's entered
 * amount/account/category (or typed description) float above the rest as a
 * group — each group keeps its time ordering, so the strongest time match wins
 * within it — and unmatched entries stay below rather than disappear. All
 * entries of the draft's type are considered (not just the recent window of
 * the field predictors) so that periodic entries outside the prediction
 * horizon can still surface.
 *
 * Entries without a description are ignored: a chip shows the description, so
 * auto-generated entries without one can't be picked. A description entered
 * today — [now]'s day, regardless of [target] — is excluded entirely: the user
 * just entered it, so it isn't suggested again even when it recurs. Recurring
 * entries are deduplicated by description, so the same thing can't fill the
 * list even when it occurs at different hours, amounts, categories, or
 * accounts. Each result carries the [RecurrenceCadence] detected for its
 * description, so a chip can label a subscription.
 */
fun predictQuickEntries(
    entries: List<Entry>,
    input: FieldPredictionInput,
    now: Instant,
    timeZone: TimeZone,
    topN: Int,
    target: Instant = now,
): List<QuickEntry> {
    val frequency = entries
        .filter { it.type == input.type && !it.description.isNullOrBlank() }
        .groupingBy { it.description!!.lowercase() }
        .eachCount()

    // Descriptions entered today are skipped: the user just typed them and
    // doesn't need them re-suggested, even when they recur monthly.
    val enteredToday = entries
        .filter { it.type == input.type }
        .filter { it.createdAt.toLocalDateTime(timeZone).date == now.toLocalDateTime(timeZone).date }
        .mapNotNull { it.description?.lowercase() }
        .toSet()

    return entries
        .filter { it.type == input.type }
        .filter { !it.description.isNullOrBlank() }
        .filter { it.description!!.lowercase() !in enteredToday }
        .filter { (frequency[it.description!!.lowercase()] ?: 0) >= 2 }
        .groupBy { it.description!!.lowercase() }
        .flatMap { (description, group) ->
            val cadence = recurringCadence(group, description)
            val due = cadence != null && isDue(group.maxOf { it.createdAt }, cadence, target)
            group.map { entry ->
                val weight = recencyWeight(entry.createdAt, now) *
                    ln(1.0 + (frequency[description] ?: 0))
                ScoredEntry(
                    entry = entry,
                    timeScore = timeAffinityScore(entry.createdAt, target, timeZone) * weight,
                    fieldScore = fieldMatchScore(entry, input) * weight,
                    cadence = cadence,
                    due = due,
                )
            }
        }
        // Recurring entries whose next occurrence lands on the target date float
        // to the very top; then entries whose fields match what the user has
        // already entered; within each group time affinity still rules.
        .sortedWith(
            compareByDescending<ScoredEntry> { it.due }
                .thenByDescending { it.fieldScore > 0.0 }
                .thenByDescending { it.timeScore }
                .thenByDescending { it.fieldScore }
                .thenBy { it.entry.id },
        )
        .distinctBy { it.entry.description.orEmpty().lowercase() }
        .take(topN)
        .map { QuickEntry(it.entry, it.cadence) }
}

/** Whether a recurring group's next occurrence (its last entry plus the interval) lands near [target]. */
private fun isDue(last: Instant, cadence: RecurrenceCadence, target: Instant): Boolean {
    val interval = when (cadence) {
        RecurrenceCadence.Weekly -> WEEKLY_DAYS.days
        RecurrenceCadence.Monthly -> MONTHLY_DAYS.days
    }
    return abs((target - (last + interval)).inWholeDays) <= DUE_WINDOW_DAYS
}

private data class ScoredEntry(
    val entry: Entry,
    val timeScore: Double,
    val fieldScore: Double,
    val cadence: RecurrenceCadence?,
    val due: Boolean,
)

/** Score of the entry's amount/category/account/description against the draft's entered fields. */
private fun fieldMatchScore(entry: Entry, input: FieldPredictionInput): Double {
    val base = baseMatchScore(entry, input.categoryId, input.accountId, input.amount) ?: 0.0
    return base + descriptionScore(entry.description, input.description)
}
