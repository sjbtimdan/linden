package org.sjbtimdan.linden.predictions

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import kotlin.math.abs
import kotlin.time.Instant

data class FieldPredictionInput(
    val type: EntryType,
    val categoryId: Long?,
    val accountId: Long?,
    val amount: Long?,
    val description: String?,
)

private const val DESCRIPTION_EXACT_WEIGHT = 3.0
private const val DESCRIPTION_PARTIAL_WEIGHT = 1.5
private const val TIME_OF_DAY_WEIGHT = 1.5
private const val TIME_OF_DAY_TOLERANCE_HOURS = 2
private const val TIME_OF_DAY_NEAR_WEIGHT = 0.75
private const val WEEKDAY_WEIGHT = 1.0
private const val MONTH_WEIGHT = 0.5

/**
 * Returns the most likely account ids for a new expense/income entry, given the
 * category, amount and description entered so far ([input]) and the last
 * [PREDICTION_HORIZON_MONTHS] months of [entries] of the same type.
 *
 * Candidates must match at least one entered signal; entries closer in time
 * (recency, hour of day, weekday, month) rank higher. No attempt is made when
 * category, amount and description are all absent.
 */
fun predictAccounts(
    entries: List<Entry>,
    input: FieldPredictionInput,
    now: Instant,
    timeZone: TimeZone,
    topN: Int,
): List<Long> {
    if (input.categoryId == null && input.amount == null && input.description.isNullOrBlank()) return emptyList()
    return predictField(entries, input.copy(accountId = null), now, timeZone, topN) { it.account.id }
}

/**
 * Returns the most likely category ids for a new expense/income entry, given the
 * account, amount and description entered so far ([input]) and the last
 * [PREDICTION_HORIZON_MONTHS] months of [entries] of the same type.
 *
 * Candidates must match at least one entered signal; entries closer in time
 * (recency, hour of day, weekday, month) rank higher. No attempt is made when
 * account, amount and description are all absent.
 */
fun predictCategories(
    entries: List<Entry>,
    input: FieldPredictionInput,
    now: Instant,
    timeZone: TimeZone,
    topN: Int,
): List<Long> {
    if (input.accountId == null && input.amount == null && input.description.isNullOrBlank()) return emptyList()
    return predictField(entries, input.copy(categoryId = null), now, timeZone, topN) { it.category?.id }
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
        // Time affinity alone never qualifies a candidate; at least one entered
        // signal must match, otherwise everything recent would be suggested.
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

private data class ScoredId(
    val id: Long,
    val score: Double,
)

private fun descriptionScore(entryDescription: String?, inputDescription: String?): Double {
    val input = inputDescription?.trim().orEmpty()
    if (input.isEmpty()) return 0.0
    val candidate = entryDescription?.trim().orEmpty()
    if (candidate.isEmpty()) return 0.0
    return when {
        candidate.equals(input, ignoreCase = true) -> DESCRIPTION_EXACT_WEIGHT

        candidate.contains(input, ignoreCase = true) || input.contains(candidate, ignoreCase = true) ->
            DESCRIPTION_PARTIAL_WEIGHT

        else -> 0.0
    }
}

/** Bonus for entries created close to [now] in hour of day, weekday and month. */
private fun timeAffinityScore(createdAt: Instant, now: Instant, timeZone: TimeZone): Double {
    val created = createdAt.toLocalDateTime(timeZone)
    val current = now.toLocalDateTime(timeZone)
    var score = 0.0
    val hourDiff = abs(created.hour - current.hour)
    score += when {
        hourDiff == 0 -> TIME_OF_DAY_WEIGHT
        hourDiff <= TIME_OF_DAY_TOLERANCE_HOURS -> TIME_OF_DAY_NEAR_WEIGHT
        else -> 0.0
    }
    if (created.dayOfWeek == current.dayOfWeek) score += WEEKDAY_WEIGHT
    if (created.monthNumber == current.monthNumber) score += MONTH_WEIGHT
    return score
}
