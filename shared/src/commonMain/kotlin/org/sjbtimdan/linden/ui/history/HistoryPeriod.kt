package org.sjbtimdan.linden.ui.history

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.sjbtimdan.linden.ui.entry.MONTHS

enum class HistoryPeriod {
    Week,
    Month,
    Year,
    All,
}

/** First day of the calendar period containing [anchor]; null for [HistoryPeriod.All]. */
fun HistoryPeriod.windowStart(anchor: LocalDate): LocalDate? = when (this) {
    HistoryPeriod.Week -> anchor.minus(anchor.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    HistoryPeriod.Month -> LocalDate(anchor.year, anchor.month.number, 1)
    HistoryPeriod.Year -> LocalDate(anchor.year, 1, 1)
    HistoryPeriod.All -> null
}

/** Last day of the calendar period containing [anchor]; null for [HistoryPeriod.All]. */
fun HistoryPeriod.windowEnd(anchor: LocalDate): LocalDate? {
    val start = windowStart(anchor) ?: return null
    val (amount, unit) = requireNotNull(step())
    return start.plus(amount, unit).minus(1, DateTimeUnit.DAY)
}

/** Anchor moved one period backwards; unchanged for [HistoryPeriod.All]. */
fun HistoryPeriod.previousAnchor(anchor: LocalDate): LocalDate {
    val (amount, unit) = step() ?: return anchor
    return anchor.minus(amount, unit)
}

/** Anchor moved one period forwards; unchanged for [HistoryPeriod.All]. */
fun HistoryPeriod.nextAnchor(anchor: LocalDate): LocalDate {
    val (amount, unit) = step() ?: return anchor
    return anchor.plus(amount, unit)
}

/** Navigator label, e.g. "10–16 Aug 2026", "Aug 2026" or "2026"; null for [HistoryPeriod.All]. */
fun HistoryPeriod.windowLabel(anchor: LocalDate): String? {
    val start = windowStart(anchor) ?: return null
    val end = windowEnd(anchor) ?: return null
    return when (this) {
        HistoryPeriod.Week -> weekLabel(start, end)
        HistoryPeriod.Month -> "${MONTHS[start.month.number - 1]} ${start.year}"
        HistoryPeriod.Year -> "${start.year}"
        HistoryPeriod.All -> null
    }
}

private fun HistoryPeriod.step(): Pair<Int, DateTimeUnit.DateBased>? = when (this) {
    HistoryPeriod.Week -> 7 to DateTimeUnit.DAY
    HistoryPeriod.Month -> 1 to DateTimeUnit.MONTH
    HistoryPeriod.Year -> 1 to DateTimeUnit.YEAR
    HistoryPeriod.All -> null
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
