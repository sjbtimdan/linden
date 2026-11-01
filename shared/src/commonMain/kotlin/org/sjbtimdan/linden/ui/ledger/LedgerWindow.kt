package org.sjbtimdan.linden.ui.ledger

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.dayIn

/** First and last day of the calendar window of a period selection; null for [LedgerPeriod.All]. */
internal data class PeriodWindow(
    val start: LocalDate,
    val end: LocalDate,
)

/**
 * Last date whose entries count while the show-future toggle is off, or null for
 * [LedgerPeriod.All] with the toggle on (no bound). Rows dated after [today] are
 * hidden only while the window spans today: a wholly past window never contains
 * them, and a wholly future window was opened to see exactly those entries.
 */
internal fun hideFutureBound(start: LocalDate?, end: LocalDate?, today: LocalDate): LocalDate? {
    if (start == null || end == null) return today
    return if (today >= start && today <= end) today else end
}

internal fun Entry.isInWindow(
    start: LocalDate?,
    end: LocalDate?,
    today: LocalDate,
    showFuture: Boolean,
    zone: TimeZone,
): Boolean {
    val date = dayIn(zone)
    val bound = if (showFuture) end else hideFutureBound(start, end, today)
    return (bound == null || date <= bound) &&
        (start == null || date >= start)
}

/**
 * Epoch-millis lower bound for a period query. Entries are re-filtered in memory
 * afterwards, so this only needs to be safe: entries in zones ahead of UTC can
 * fall up to 14 hours before UTC midnight of [this], hence the one-day margin.
 */
internal fun LocalDate.sqlLowerBound(): Long =
    minus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

/**
 * Epoch-millis upper bound for a period query. Entries are re-filtered in memory
 * afterwards, so this only needs to be safe: entries in zones behind UTC can fall
 * up to ~14 hours after UTC midnight of the day after [this], hence the two-day margin.
 */
internal fun LocalDate.sqlUpperBound(): Long =
    plus(2, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
