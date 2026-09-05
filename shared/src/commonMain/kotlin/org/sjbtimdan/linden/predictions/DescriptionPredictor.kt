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
    val description: String?,
)

/**
 * Returns the most likely descriptions for a new entry, based on the last
 * [PREDICTION_HORIZON_MONTHS] months of [entries] of the same type as [input.type].
 *
 * Missing inputs are skipped (best effort); no attempt is made when category,
 * account, amount and description are all absent. A typed description narrows
 * the candidates to descriptions matching it, so rare descriptions surface as
 * soon as their text is entered. Recent entries are weighted higher.
 */
fun predictDescriptions(
    entries: List<Entry>,
    input: DescriptionPredictionInput,
    now: Instant,
    timeZone: TimeZone,
    topN: Int,
): List<String> {
    val query = input.description?.trim().orEmpty()
    if (input.categoryId == null && input.accountId == null && input.amount == null && query.isEmpty()) {
        return emptyList()
    }
    return candidateEntries(entries, input.type, now, timeZone)
        .mapNotNull { candidate ->
            val description = candidate.description?.trim().orEmpty().ifEmpty { return@mapNotNull null }
            val match = baseMatchScore(candidate, input.categoryId, input.accountId, input.amount) ?: 0.0
            val descriptionMatch = descriptionScore(description, input.description)
            // A typed description must match; otherwise at least one entered field must.
            if (query.isNotEmpty()) {
                if (descriptionMatch == 0.0) return@mapNotNull null
            } else if (match == 0.0) {
                return@mapNotNull null
            }
            ScoredDescription(description, (match + descriptionMatch) * recencyWeight(candidate.createdAt, now))
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
