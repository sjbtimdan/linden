package org.sjbtimdan.linden.predictions

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import kotlin.math.abs
import kotlin.math.pow
import kotlin.time.Instant

const val PREDICTION_HORIZON_MONTHS = 6
const val PREDICTION_TOP_N = 10

internal const val AMOUNT_EXACT_WEIGHT = 4.0
internal const val AMOUNT_NEAR_WEIGHT = 2.0
internal const val CATEGORY_WEIGHT = 3.0
internal const val ACCOUNT_WEIGHT = 2.0
internal const val AMOUNT_TOLERANCE = 0.1
internal const val RECENCY_DECAY_PER_MONTH = 0.85
internal const val DAYS_PER_MONTH = 30.44
internal const val DESCRIPTION_EXACT_WEIGHT = 3.0
internal const val DESCRIPTION_PARTIAL_WEIGHT = 1.5
internal const val TIME_OF_DAY_WEIGHT = 1.5
internal const val TIME_OF_DAY_TOLERANCE_HOURS = 2
internal const val TIME_OF_DAY_NEAR_WEIGHT = 0.75
internal const val WEEKDAY_WEIGHT = 1.0
internal const val MONTH_WEIGHT = 0.5
internal const val DAY_OF_MONTH_WEIGHT = 1.0
internal const val DAY_OF_MONTH_TOLERANCE_DAYS = 2
internal const val DAY_OF_MONTH_NEAR_WEIGHT = 0.5

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

/** Score of the entry's description against the typed input: exact beats partial. */
internal fun descriptionScore(entryDescription: String?, inputDescription: String?): Double {
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

/**
 * Bonus for entries created close to [now] in hour of day, weekday, month and
 * day of month — the signals that repeat across dates. Day of month matters for
 * monthly bills (e.g. rent on the 2nd), which otherwise get no affinity bonus
 * because their weekday and month drift between occurrences.
 */
internal fun timeAffinityScore(createdAt: Instant, now: Instant, timeZone: TimeZone): Double {
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
    if (created.month == current.month) score += MONTH_WEIGHT
    val dayDiff = abs(created.day - current.day)
    score += when {
        dayDiff == 0 -> DAY_OF_MONTH_WEIGHT
        dayDiff <= DAY_OF_MONTH_TOLERANCE_DAYS -> DAY_OF_MONTH_NEAR_WEIGHT
        else -> 0.0
    }
    return score
}
