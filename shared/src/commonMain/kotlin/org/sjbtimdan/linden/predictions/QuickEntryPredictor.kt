package org.sjbtimdan.linden.predictions

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Entry
import kotlin.math.ln
import kotlin.time.Instant

const val QUICK_ENTRY_TOP_N = 5

/** A whole entry the user is likely to repeat right now, with its detected cadence. */
data class QuickEntry(
    val entry: Entry,
    val cadence: RecurrenceCadence?,
)

/**
 * Returns the whole entries a new entry is most likely to repeat right now.
 *
 * Candidates are ranked by time of day — hour, weekday, month — multiplied by
 * recency decay and a logarithmic frequency weight so that recent,
 * frequently-entered entries dominate. Entries that appear only once are
 * filtered out. The draft's entered fields break ties within a time tier. All
 * entries of the draft's type are considered (not just the recent window of
 * the field predictors) so that periodic entries outside the prediction
 * horizon can still surface.
 *
 * Entries without a description are ignored: a chip shows the description, so
 * auto-generated entries without one can't be picked. Recurring entries are
 * deduplicated by description and hour of day, so the same thing at the same
 * clock time can't fill the list — even when its amount, category or account
 * drifted between occurrences. Each result carries the [RecurrenceCadence]
 * detected for its description, so a chip can label a subscription.
 */
fun predictQuickEntries(
    entries: List<Entry>,
    input: FieldPredictionInput,
    now: Instant,
    timeZone: TimeZone,
    topN: Int,
): List<QuickEntry> {
    val frequency = entries
        .filter { it.type == input.type && !it.description.isNullOrBlank() }
        .groupingBy { it.description!!.lowercase() }
        .eachCount()

    return entries.asSequence()
        .filter { it.type == input.type }
        .filter { !it.description.isNullOrBlank() }
        .filter { (frequency[it.description!!.lowercase()] ?: 0) >= 2 }
        .map { entry ->
            val weight = recencyWeight(entry.createdAt, now) *
                ln(1.0 + (frequency[entry.description!!.lowercase()] ?: 0))
            ScoredEntry(
                entry = entry,
                timeScore = timeAffinityScore(entry.createdAt, now, timeZone) * weight,
                fieldScore = fieldMatchScore(entry, input) * weight,
            )
        }
        .sortedWith(
            compareByDescending<ScoredEntry> { it.timeScore }
                .thenByDescending { it.fieldScore }
                .thenBy { it.entry.id },
        )
        .distinctBy { it.entry.quickEntryKey(timeZone) }
        .take(topN)
        .map { scored ->
            QuickEntry(
                entry = scored.entry,
                cadence = recurringCadence(entries, scored.entry.description.orEmpty()),
            )
        }
        .toList()
}

private data class ScoredEntry(
    val entry: Entry,
    val timeScore: Double,
    val fieldScore: Double,
)

/** What makes two entries the same recurring thing: the description at the same clock hour. */
private data class QuickEntryKey(
    val description: String,
    val hourOfDay: Int,
)

private fun Entry.quickEntryKey(timeZone: TimeZone): QuickEntryKey =
    QuickEntryKey(description.orEmpty(), createdAt.toLocalDateTime(timeZone).hour)

/** Score of the entry's amount/category/account/description against the draft's entered fields. */
private fun fieldMatchScore(entry: Entry, input: FieldPredictionInput): Double {
    val base = baseMatchScore(entry, input.categoryId, input.accountId, input.amount) ?: 0.0
    return base + descriptionScore(entry.description, input.description)
}
