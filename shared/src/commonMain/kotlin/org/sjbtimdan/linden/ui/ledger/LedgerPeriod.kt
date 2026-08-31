package org.sjbtimdan.linden.ui.ledger

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.sjbtimdan.linden.ui.entry.MONTHS

enum class LedgerPeriod {
    Day,
    Week,
    Month,
    Year,
    All,
}

/** First day of the calendar period containing [anchor]; null for [LedgerPeriod.All]. */
fun LedgerPeriod.windowStart(anchor: LocalDate): LocalDate? = when (this) {
    LedgerPeriod.Day -> anchor
    LedgerPeriod.Week -> anchor.minus(anchor.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    LedgerPeriod.Month -> LocalDate(anchor.year, anchor.month.number, 1)
    LedgerPeriod.Year -> LocalDate(anchor.year, 1, 1)
    LedgerPeriod.All -> null
}

/** Last day of the calendar period containing [anchor]; null for [LedgerPeriod.All]. */
fun LedgerPeriod.windowEnd(anchor: LocalDate): LocalDate? {
    val start = windowStart(anchor) ?: return null
    val (amount, unit) = requireNotNull(step())
    return start.plus(amount, unit).minus(1, DateTimeUnit.DAY)
}

/**
 * Whether the period's window contains [date]. [LedgerPeriod.All] has no window, so it
 * always contains every date. For bounded periods this is true when [date] falls within
 * the window's first and last day — i.e. the period is the current one.
 */
fun LedgerPeriod.includes(date: LocalDate, anchor: LocalDate): Boolean {
    val start = windowStart(anchor) ?: return true
    val end = windowEnd(anchor) ?: return true
    return date >= start && date <= end
}

/** Anchor moved one period backwards; unchanged for [LedgerPeriod.All]. */
fun LedgerPeriod.previousAnchor(anchor: LocalDate): LocalDate {
    val (amount, unit) = step() ?: return anchor
    return anchor.minus(amount, unit)
}

/** Anchor moved one period forwards; unchanged for [LedgerPeriod.All]. */
fun LedgerPeriod.nextAnchor(anchor: LocalDate): LocalDate {
    val (amount, unit) = step() ?: return anchor
    return anchor.plus(amount, unit)
}

/** Navigator label, e.g. "15 Aug 2026", "10–16 Aug 2026", "Aug 2026" or "2026"; null for [LedgerPeriod.All]. */
fun LedgerPeriod.windowLabel(anchor: LocalDate): String? {
    val start = windowStart(anchor) ?: return null
    val end = windowEnd(anchor) ?: return null
    return when (this) {
        LedgerPeriod.Day -> "${start.day} ${MONTHS[start.month.number - 1]} ${start.year}"
        LedgerPeriod.Week -> weekLabel(start, end)
        LedgerPeriod.Month -> "${MONTHS[start.month.number - 1]} ${start.year}"
        LedgerPeriod.Year -> "${start.year}"
        LedgerPeriod.All -> null
    }
}

private fun LedgerPeriod.step(): Pair<Int, DateTimeUnit.DateBased>? = when (this) {
    LedgerPeriod.Day -> 1 to DateTimeUnit.DAY
    LedgerPeriod.Week -> 7 to DateTimeUnit.DAY
    LedgerPeriod.Month -> 1 to DateTimeUnit.MONTH
    LedgerPeriod.Year -> 1 to DateTimeUnit.YEAR
    LedgerPeriod.All -> null
}

private fun weekLabel(start: LocalDate, end: LocalDate): String {
    val startMonth = MONTHS[start.month.number - 1]
    val endMonth = MONTHS[end.month.number - 1]
    return when {
        start.year != end.year ->
            "${start.day} $startMonth ${start.year} – ${end.day} $endMonth ${end.year}"

        start.month != end.month ->
            "${start.day} $startMonth – ${end.day} $endMonth ${start.year}"

        else -> "${start.day}–${end.day} $startMonth ${start.year}"
    }
}
