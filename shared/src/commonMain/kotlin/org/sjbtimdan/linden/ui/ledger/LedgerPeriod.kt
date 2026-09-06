package org.sjbtimdan.linden.ui.ledger

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.sjbtimdan.linden.ui.entry.DateLanguage
import org.sjbtimdan.linden.ui.entry.dateLanguage
import org.sjbtimdan.linden.ui.entry.platformLanguageCode

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

/**
 * Navigator label per the active language, e.g. "Aug 15, 2026", "Aug 10–16, 2026",
 * "Aug 2026" or "2026"; null for [LedgerPeriod.All].
 */
fun LedgerPeriod.windowLabel(anchor: LocalDate): String? = windowLabel(anchor, dateLanguage(platformLanguageCode()))

/** Language-explicit variant of [windowLabel], for tests and callers that resolved the language. */
internal fun LedgerPeriod.windowLabel(anchor: LocalDate, language: DateLanguage): String? {
    val start = windowStart(anchor) ?: return null
    val end = windowEnd(anchor) ?: return null
    return when (this) {
        LedgerPeriod.Day -> language.dateText(start.day, start.month.number, start.year)
        LedgerPeriod.Week -> weekLabel(start, end, language)
        LedgerPeriod.Month -> language.monthYearText(start.month.number, start.year)
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

private fun weekLabel(start: LocalDate, end: LocalDate, language: DateLanguage): String {
    val startMonth = language.monthShort(start.month.number)
    val endMonth = language.monthShort(end.month.number)
    return when {
        start.year != end.year -> when (language) {
            DateLanguage.English ->
                "$startMonth ${start.day}, ${start.year} – $endMonth ${end.day}, ${end.year}"

            DateLanguage.Italian ->
                "${start.day} $startMonth ${start.year} – ${end.day} $endMonth ${end.year}"

            DateLanguage.Chinese ->
                "${start.year}年$startMonth${start.day}日 – ${end.year}年$endMonth${end.day}日"
        }

        start.month != end.month -> when (language) {
            DateLanguage.English ->
                "$startMonth ${start.day} – $endMonth ${end.day}, ${start.year}"

            DateLanguage.Italian ->
                "${start.day} $startMonth – ${end.day} $endMonth ${start.year}"

            DateLanguage.Chinese ->
                "${start.year}年$startMonth${start.day}日 – $endMonth${end.day}日"
        }

        else -> when (language) {
            DateLanguage.English ->
                "$startMonth ${start.day}–${end.day}, ${start.year}"

            DateLanguage.Italian ->
                "${start.day}–${end.day} $startMonth ${start.year}"

            DateLanguage.Chinese ->
                "${start.year}年$startMonth${start.day}日–${end.day}日"
        }
    }
}
