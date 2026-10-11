package org.sjbtimdan.linden.inflation

import kotlinx.datetime.LocalDate
import kotlin.math.abs

private const val PPM = 1_000_000L

/**
 * Parts-per-million factor that restates [date]'s money in [series]' currency
 * as the latest stored month's money (10650 -> 10000 means "6.5% more"), or
 * null when no series is available. Integer-only math: one ppm unit is 1/1e6.
 */
fun inflationFactorPpm(series: CpiSeries?, date: LocalDate): Long? {
    if (series == null) return null
    val then = series.indexAt(date)
    if (then <= 0) return null
    return series.values.last().toLong() * PPM / then
}

/** [amount] scaled by a parts-per-million [factor], rounded half-up; the sign is preserved. */
fun adjustMinor(amount: Long, factor: Long): Long {
    val adjusted = (abs(amount) * factor + PPM / 2) / PPM
    return if (amount < 0) -adjusted else adjusted
}
