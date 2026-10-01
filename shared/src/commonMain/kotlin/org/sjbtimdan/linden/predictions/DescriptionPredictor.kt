package org.sjbtimdan.linden.predictions

import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import kotlin.math.ln
import kotlin.time.Instant

data class DescriptionPredictionInput(
    val type: EntryType,
    val categoryId: Long?,
    val accountId: Long?,
    val amount: Long?,
    val description: String?,
)

/**
 * Returns the most likely descriptions for a new entry, based on [entries] of
 * the same type as [input.type]; older entries are weighted lower by recency
 * decay rather than cut off by a horizon.
 *
 * While no description is typed, a selected category constrains the candidates
 * to that category — falling back to all entries when the category has no
 * history — so suggestions don't drift to frequent entries from other
 * categories. Once text is typed, matching descriptions are considered
 * regardless of category (the category still scores them higher), so a merchant
 * logged under another category can still surface.
 *
 * Missing inputs are skipped (best effort); no attempt is made when category,
 * account, amount and description are all absent. A typed description narrows
 * the candidates to descriptions matching it, so rare descriptions surface as
 * soon as their text is entered.
 *
 * A description's score is its best occurrence plus a logarithmic frequency
 * bonus: one recent occurrence matching every entered field can outrank a
 * description repeated often but matching only loosely, while repeated
 * descriptions still beat one-offs.
 */
fun predictDescriptions(
    entries: List<Entry>,
    input: DescriptionPredictionInput,
    now: Instant,
    topN: Int,
): List<String> {
    val query = input.description?.trim().orEmpty()
    if (input.categoryId == null && input.accountId == null && input.amount == null && query.isEmpty()) {
        return emptyList()
    }
    val sameType = entries.filter { it.type == input.type }
    val candidates = if (query.isEmpty() && input.categoryId != null) {
        sameType.filter { it.category?.id == input.categoryId }.ifEmpty { sameType }
    } else {
        sameType
    }
    return candidates.asSequence()
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
            val best = group.maxBy { it.score }
            ScoredDescription(
                description = best.description,
                score = best.score + DESCRIPTION_FREQUENCY_PRIOR_WEIGHT * ln(1.0 + group.size),
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
