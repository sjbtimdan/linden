package org.sjbtimdan.linden.predictions

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import kotlin.math.abs
import kotlin.math.pow
import kotlin.time.Instant

const val PREDICTION_HORIZON_MONTHS = 6
const val PREDICTION_TOP_N = 5

internal const val AMOUNT_EXACT_WEIGHT = 4.0
internal const val AMOUNT_NEAR_WEIGHT = 2.0
internal const val CATEGORY_WEIGHT = 3.0
internal const val ACCOUNT_WEIGHT = 2.0
internal const val AMOUNT_TOLERANCE = 0.1
internal const val RECENCY_DECAY_PER_MONTH = 0.85
internal const val DAYS_PER_MONTH = 30.44

/** Entries of [type] from the last [PREDICTION_HORIZON_MONTHS] months, in list order. */
internal fun candidateEntries(
    entries: List<Entry>,
    type: EntryType,
    now: Instant,
    timeZone: TimeZone,
): Sequence<Entry> {
    val cutoff = now.minus(PREDICTION_HORIZON_MONTHS, DateTimeUnit.MONTH, timeZone)
    return entries.asSequence()
        .filter { it.type == type }
        .filter { it.createdAt >= cutoff }
}

/**
 * Score of an entry's amount/category/account against the given inputs, or null
 * when none of them match. Missing inputs are skipped.
 */
internal fun baseMatchScore(entry: Entry, categoryId: Long?, accountId: Long?, amount: Long?): Double? {
    var score = 0.0
    amount?.let { inputAmount ->
        when {
            entry.amount == inputAmount -> score += AMOUNT_EXACT_WEIGHT
            isWithinTolerance(entry.amount, inputAmount) -> score += AMOUNT_NEAR_WEIGHT
        }
    }
    if (categoryId != null && entry.category?.id == categoryId) score += CATEGORY_WEIGHT
    if (accountId != null && entry.account.id == accountId) score += ACCOUNT_WEIGHT
    return score.takeIf { it > 0.0 }
}

internal fun isWithinTolerance(historyAmount: Long, inputAmount: Long): Boolean {
    if (historyAmount <= 0) return false
    val difference = abs(historyAmount - inputAmount).toDouble()
    return difference / historyAmount <= AMOUNT_TOLERANCE
}

internal fun recencyWeight(createdAt: Instant, now: Instant): Double {
    val months = ((now - createdAt).inWholeDays.toDouble() / DAYS_PER_MONTH).coerceAtLeast(0.0)
    return RECENCY_DECAY_PER_MONTH.pow(months)
}
