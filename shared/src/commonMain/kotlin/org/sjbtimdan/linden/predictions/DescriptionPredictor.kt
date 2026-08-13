package org.sjbtimdan.linden.predictions

import kotlin.math.abs
import kotlin.math.pow
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType

const val PREDICTION_HORIZON_MONTHS = 6
const val PREDICTION_TOP_N = 5

data class DescriptionPredictionInput(
    val type: EntryType,
    val categoryId: Long?,
    val accountId: Long?,
    val amount: Long?,
)

private const val AMOUNT_EXACT_WEIGHT = 4.0
private const val AMOUNT_NEAR_WEIGHT = 2.0
private const val CATEGORY_WEIGHT = 3.0
private const val ACCOUNT_WEIGHT = 2.0
private const val AMOUNT_TOLERANCE = 0.1
private const val RECENCY_DECAY_PER_MONTH = 0.85
private const val DAYS_PER_MONTH = 30.44

/**
 * Returns the most likely descriptions for a new entry, based on the last
 * [PREDICTION_HORIZON_MONTHS] months of [entries] of the same type as [input.type].
 *
 * Missing inputs are skipped (best effort); no attempt is made when category,
 * account and amount are all absent. Recent entries are weighted higher.
 */
fun predictDescriptions(
    entries: List<Entry>,
    input: DescriptionPredictionInput,
    now: Instant,
    timeZone: TimeZone,
    topN: Int,
): List<String> {
    if (input.categoryId == null && input.accountId == null && input.amount == null) return emptyList()
    val cutoff = now.minus(PREDICTION_HORIZON_MONTHS, DateTimeUnit.MONTH, timeZone)
    return entries.asSequence()
        .filter { it.type == input.type }
        .filter { it.createdAt >= cutoff }
        .mapNotNull { candidate ->
            val description = candidate.description?.trim().orEmpty().ifEmpty { return@mapNotNull null }
            val match = matchScore(candidate, input) ?: return@mapNotNull null
            ScoredDescription(description, match * recencyWeight(candidate.createdAt, now))
        }
        .groupBy { it.description.lowercase() }
        .map { (_, group) ->
            ScoredDescription(
                description = group.maxBy { it.score }.description,
                score = group.sumOf { it.score },
            )
        }
        .sortedWith(compareByDescending<ScoredDescription> { it.score }.thenBy { it.description })
        .take(topN)
        .map { it.description }
}

private data class ScoredDescription(
    val description: String,
    val score: Double,
)

private fun matchScore(entry: Entry, input: DescriptionPredictionInput): Double? {
    var score = 0.0
    input.amount?.let { amount ->
        when {
            entry.amount == amount -> score += AMOUNT_EXACT_WEIGHT
            isWithinTolerance(entry.amount, amount) -> score += AMOUNT_NEAR_WEIGHT
        }
    }
    if (input.categoryId != null && entry.category?.id == input.categoryId) score += CATEGORY_WEIGHT
    if (input.accountId != null && entry.account.id == input.accountId) score += ACCOUNT_WEIGHT
    return score.takeIf { it > 0.0 }
}

private fun isWithinTolerance(historyAmount: Long, inputAmount: Long): Boolean {
    if (historyAmount <= 0) return false
    val difference = abs(historyAmount - inputAmount).toDouble()
    return difference / historyAmount <= AMOUNT_TOLERANCE
}

private fun recencyWeight(createdAt: Instant, now: Instant): Double {
    val months = ((now - createdAt).inWholeDays.toDouble() / DAYS_PER_MONTH).coerceAtLeast(0.0)
    return RECENCY_DECAY_PER_MONTH.pow(months)
}
