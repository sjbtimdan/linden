package org.sjbtimdan.linden.predictions

import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import kotlin.time.Instant

data class DescriptionPredictionInput(
    val type: EntryType,
    val categoryId: Long?,
    val accountId: Long?,
    val amount: Long?,
)

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
    return candidateEntries(entries, input.type, now, timeZone)
        .mapNotNull { candidate ->
            val description = candidate.description?.trim().orEmpty().ifEmpty { return@mapNotNull null }
            val match = baseMatchScore(candidate, input.categoryId, input.accountId, input.amount)
                ?: return@mapNotNull null
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
