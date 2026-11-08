package org.sjbtimdan.linden.predictions

import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import kotlin.time.Instant

data class FieldPredictionInput(
    val type: EntryType,
    val categoryId: Long?,
    val accountId: Long?,
    val amount: Long?,
    val description: String?,
)

/**
 * Returns the most likely account ids for a new expense/income entry, given the
 * category, amount and description entered so far ([input]) and the last
 * [PREDICTION_HORIZON_MONTHS] months of [entries] of the same type.
 *
 * When at least one signal (category, amount or description) is present,
 * candidates must match that signal; entries closer in time rank higher.
 * When no signals are present, results are ranked by usage frequency weighted
 * by recency so the most commonly used options appear first.
 *
 * With no history at all ([entries] empty) [fallback] is returned instead, so a
 * cold start can still show the seeded defaults as "Recommended".
 */
fun predictAccounts(
    entries: List<Entry>,
    input: FieldPredictionInput,
    now: Instant,
    timeZone: TimeZone,
    topN: Int,
    fallback: List<Long> = emptyList(),
): List<Long> {
    if (entries.isEmpty()) return fallback.take(topN)
    val hasSignal = input.categoryId != null || input.amount != null || !input.description.isNullOrBlank()
    if (!hasSignal) return frequencyRankIds(entries, input.type, now, timeZone, topN) { it.account.id }
    return predictField(entries, input.copy(accountId = null), now, timeZone, topN) { it.account.id }
}

/**
 * Returns the most likely category ids for a new expense/income entry, given the
 * account, amount and description entered so far ([input]) and the last
 * [PREDICTION_HORIZON_MONTHS] months of [entries] of the same type.
 *
 * When at least one signal (account, amount or description) is present,
 * candidates must match that signal; entries closer in time rank higher.
 * When no signals are present, results are ranked by usage frequency weighted
 * by recency so the most commonly used options appear first.
 *
 * With no history at all ([entries] empty) [fallback] is returned instead, so a
 * cold start can still show the seeded defaults as "Recommended".
 */
fun predictCategories(
    entries: List<Entry>,
    input: FieldPredictionInput,
    now: Instant,
    timeZone: TimeZone,
    topN: Int,
    fallback: List<Long> = emptyList(),
): List<Long> {
    if (entries.isEmpty()) return fallback.take(topN)
    val hasSignal = input.accountId != null || input.amount != null || !input.description.isNullOrBlank()
    if (!hasSignal) return frequencyRankIds(entries, input.type, now, timeZone, topN) { it.category?.id }
    return predictField(entries, input.copy(categoryId = null), now, timeZone, topN) { it.category?.id }
}

/**
 * The category a new expense/income entry should default to when [accountId] is
 * selected: that account's most-used category from the last
 * [PREDICTION_HORIZON_MONTHS] months, ranked by usage frequency weighted by
 * recency. Returns null when the account has no history, so a cold start never
 * guesses.
 *
 * A [currentCategoryId] that is already a category used with [accountId]
 * survives untouched — a deliberate, compatible choice is never stomped. An
 * empty or stale category (one never used with this account) is replaced by the
 * account's most-used one.
 */
fun autoCategoryForAccount(
    entries: List<Entry>,
    type: EntryType,
    accountId: Long,
    currentCategoryId: Long?,
    now: Instant,
    timeZone: TimeZone,
): Long? {
    val accountEntries = candidateEntries(entries, type, now, timeZone)
        .filter { it.account.id == accountId }
        .toList()
    if (accountEntries.isEmpty()) return null
    val top = accountEntries
        .mapNotNull { entry -> entry.category?.id?.let { id -> id to recencyWeight(entry.createdAt, now) } }
        .groupBy { it.first }
        .map { (id, scored) -> id to scored.sumOf { it.second } }
        .sortedWith(compareByDescending<Pair<Long, Double>> { it.second }.thenBy { it.first })
        .firstOrNull()
        ?.first
        ?: return null
    return when {
        currentCategoryId == null -> top
        accountEntries.any { it.category?.id == currentCategoryId } -> currentCategoryId
        else -> top
    }
}

private fun predictField(
    entries: List<Entry>,
    input: FieldPredictionInput,
    now: Instant,
    timeZone: TimeZone,
    topN: Int,
    predictedId: (Entry) -> Long?,
): List<Long> = candidateEntries(entries, input.type, now, timeZone)
    .mapNotNull { candidate ->
        val id = predictedId(candidate) ?: return@mapNotNull null
        val match = baseMatchScore(candidate, input.categoryId, input.accountId, input.amount) ?: 0.0
        val description = descriptionScore(candidate.description, input.description)
        // At least one entered signal must match — time affinity alone would
        // suggest everything recent.
        if (match == 0.0 && description == 0.0) return@mapNotNull null
        val score = (match + description + timeAffinityScore(candidate.createdAt, now, timeZone)) *
            recencyWeight(candidate.createdAt, now)
        ScoredId(id, score)
    }
    .groupBy { it.id }
    .map { (id, group) -> ScoredId(id, group.sumOf { it.score }) }
    .sortedWith(compareByDescending<ScoredId> { it.score }.thenBy { it.id })
    .take(topN)
    .map { it.id }

/**
 * Ranks all ids of [type] by usage frequency in [entries], weighted by recency.
 * Used as a fallback when the draft has no context (no category/amount/description
 * entered yet) so that the most commonly used options appear first instead of
 * an empty suggestion list.
 */
private fun frequencyRankIds(
    entries: List<Entry>,
    type: EntryType,
    now: Instant,
    timeZone: TimeZone,
    topN: Int,
    idOf: (Entry) -> Long?,
): List<Long> = candidateEntries(entries, type, now, timeZone)
    .mapNotNull { entry ->
        val id = idOf(entry) ?: return@mapNotNull null
        ScoredId(id, recencyWeight(entry.createdAt, now))
    }
    .groupBy { it.id }
    .map { (id, group) -> ScoredId(id, group.sumOf { it.score }) }
    .sortedWith(compareByDescending<ScoredId> { it.score }.thenBy { it.id })
    .take(topN)
    .map { it.id }

private data class ScoredId(
    val id: Long,
    val score: Double,
)
