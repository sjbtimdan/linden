package org.sjbtimdan.linden.predictions

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.TransferEntry
import kotlin.math.abs
import kotlin.math.ln
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

const val QUICK_ENTRY_TOP_N = 5

/** How many variants of one description may fill quick-entry chips. */
const val QUICK_ENTRY_MAX_VARIANTS_PER_DESCRIPTION = 2

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
 * dating an entry to a recurring bill's usual day surfaces that bill. A
 * monthly description that already has an occurrence within [DUE_WINDOW_DAYS]
 * of [target] is not suggested at all — that date is already covered — while
 * everything else stays suggestible: weekly cycles are short enough that the
 * due tier already covers them, and non-recurring descriptions remain
 * repeatable. The remaining candidates are ranked by time of day — hour,
 * weekday, month and day of month — against [target], the moment the new entry
 * is dated, multiplied by recency decay against [now] and a logarithmic
 * frequency weight so that recent, frequently-entered entries dominate.
 * Descriptions with fewer than two entries are filtered out.
 *
 * Entries are identified by description, amount and source account (and a
 * transfer's destination), not by description alone. That keeps two coffee
 * vendors with the same wording but different prices as separate candidates,
 * and a description entered today only hides the exact variant it repeats —
 * another vendor's variant stays available. When a description has several
 * variants, only variants that recur (the same amount and account seen at
 * least twice) stay in the list, plus the highest-ranked one: a lumped
 * "Groceries" series whose total changes every time collapses to one chip,
 * while two recurring coffee prices remain separate. A description still fills
 * at most [QUICK_ENTRY_MAX_VARIANTS_PER_DESCRIPTION] chips — its highest-ranked
 * variants — so three recurring ATM totals leave room for other descriptions.
 * Descriptions that differ only in case, whitespace or punctuation ("ATM .",
 * "ATM.", "ATM") form one family for that cap. Entries matching the draft's
 * entered amount/account/category (or typed description) float above the rest
 * as a group — each group keeps its time ordering, so the strongest time match
 * wins within it — and unmatched entries stay below rather than disappear.
 * All entries of the draft's type are considered (not just the recent window
 * of the field predictors) so that periodic entries outside the prediction
 * horizon can still surface.
 *
 * Entries without a description are ignored: a chip shows the description, so
 * auto-generated entries without one can't be picked. Each result carries the
 * [RecurrenceCadence] detected for its description, so a chip can label a
 * subscription.
 */
fun predictQuickEntries(
    entries: List<Entry>,
    input: FieldPredictionInput,
    now: Instant,
    timeZone: TimeZone,
    topN: Int,
    target: Instant = now,
): List<QuickEntry> {
    val typed = entries.filter { it.type == input.type && !it.description.isNullOrBlank() }
    val descriptionFrequency = typed
        .groupingBy { it.description!!.lowercase() }
        .eachCount()
    val variantFrequency = typed
        .groupingBy(::variantKey)
        .eachCount()
    val byDescription = typed.groupBy { it.description!!.lowercase() }
    val cadenceByDescription = byDescription.mapValues { (description, group) ->
        recurringCadence(group, description)
    }
    val lastByDescription = byDescription.mapValues { (_, group) -> group.maxOf { it.createdAt } }

    val today = now.toLocalDateTime(timeZone).date
    val enteredToday = typed
        .filter { it.createdAt.toLocalDateTime(timeZone).date == today }
        .map(::variantKey)
        .toSet()

    val scored = typed
        .filter { variantKey(it) !in enteredToday }
        .filter { (descriptionFrequency[it.description!!.lowercase()] ?: 0) >= 2 }
        .groupBy(::variantKey)
        .flatMap { (key, group) ->
            val description = group.first().description!!.lowercase()
            val cadence = cadenceByDescription[description]
            val loggedNearTarget = cadence == RecurrenceCadence.Monthly &&
                byDescription[description].orEmpty().any { withinTargetWindow(it.createdAt, target) }
            if (loggedNearTarget) {
                emptyList()
            } else {
                val last = lastByDescription[description] ?: group.maxOf { it.createdAt }
                val due = cadence != null && isDue(last, cadence, target)
                group.map { entry ->
                    val weight = recencyWeight(entry.createdAt, now) *
                        ln(1.0 + (variantFrequency[key] ?: 0))
                    ScoredEntry(
                        entry = entry,
                        timeScore = timeAffinityScore(entry.createdAt, target, timeZone) * weight,
                        fieldScore = fieldMatchScore(entry, input) * weight,
                        cadence = cadence,
                        due = due,
                    )
                }
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

    // Keep one chip for a description with only one candidate. When a
    // description has several candidates, only variants that recur (the same
    // amount and account seen twice) survive: a lumped "Groceries" series with
    // a new total every time collapses back to one chip, while two recurring
    // coffee prices stay separate. The top-ranked variant always survives, so
    // a brand-new variant is still available. Deduplicating variants first
    // keeps a variant's history from consuming the per-description cap twice.
    val distinctVariants = scored.distinctBy { variantKey(it.entry) }
    val variants = groupScoredVariants(distinctVariants)
    val recurringVariants = variantFrequency.filterValues { it >= 2 }.keys
    val kept = distinctVariants.filter { scoredEntry ->
        val description = scoredEntry.entry.description.orEmpty().lowercase()
        val variant = variantKey(scoredEntry.entry)
        variants.multiple[description] != true ||
            variant == variants.first[description] ||
            variant in recurringVariants
    }
    return kept
        .withIndex()
        .groupBy { (_, scoredEntry) -> descriptionFamily(scoredEntry.entry.description) }
        .values
        .flatMap { family -> family.take(QUICK_ENTRY_MAX_VARIANTS_PER_DESCRIPTION) }
        .sortedBy { it.index }
        .take(topN)
        .map { QuickEntry(it.value.entry, it.value.cadence) }
}

private data class ScoredVariants(
    val first: Map<String, String>,
    val multiple: Map<String, Boolean>,
)

private fun groupScoredVariants(entries: List<ScoredEntry>): ScoredVariants {
    val byDescription = entries.groupBy { it.entry.description.orEmpty().lowercase() }
    return ScoredVariants(
        first = byDescription.mapValues { (_, group) -> variantKey(group.first().entry) },
        multiple = byDescription.mapValues { (_, group) ->
            group.map { variantKey(it.entry) }.distinct().size > 1
        },
    )
}

/**
 * Identity of a repeatable entry: the same words, amount and source account
 * (plus the destination for a transfer) are one candidate; changing any of
 * them makes a separate variant, such as the same coffee bought at two shops.
 */
private fun variantKey(entry: Entry): String {
    val destination = (entry as? TransferEntry)?.toAccount?.id ?: -1L
    return "${entry.description.orEmpty().lowercase()}|${entry.amount}|${entry.account.id}|$destination"
}

/**
 * Grouping key for the per-description chip cap: descriptions that differ only
 * in case, whitespace or trailing punctuation are one merchant, so their
 * variants share the limit. [variantKey] still keeps their amounts separate.
 */
internal fun descriptionFamily(description: String?): String = description.orEmpty()
    .trim()
    .lowercase()
    .replace(COLLAPSED_WHITESPACE, " ")
    .replace(WHITESPACE_BEFORE_PUNCTUATION) { it.groupValues[1] }
    .replace(TRAILING_PUNCTUATION, "")
    .trim()

private val COLLAPSED_WHITESPACE = Regex("\\s+")
private val WHITESPACE_BEFORE_PUNCTUATION = Regex("\\s+([.,;:!?])")
private val TRAILING_PUNCTUATION = Regex("[.,;:!?]+$")

/** Whether [instant] falls within [DUE_WINDOW_DAYS] of [target]. */
private fun withinTargetWindow(instant: Instant, target: Instant): Boolean =
    abs((instant - target).inWholeDays) <= DUE_WINDOW_DAYS

/** Whether a recurring group's next occurrence (its last entry plus the interval) lands near [target]. */
private fun isDue(last: Instant, cadence: RecurrenceCadence, target: Instant): Boolean {
    val interval = when (cadence) {
        RecurrenceCadence.Weekly -> WEEKLY_DAYS.days
        RecurrenceCadence.Monthly -> MONTHLY_DAYS.days
    }
    return withinTargetWindow(last + interval, target)
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
