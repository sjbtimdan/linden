package org.sjbtimdan.linden.ui.inflation

import org.sjbtimdan.linden.inflation.CpiSeries
import org.sjbtimdan.linden.model.Currency
import kotlin.math.abs
import kotlin.math.round

/** One currency's CPI summary in the inflation-data viewer. */
data class InflationRow(
    val currency: Currency,
    val firstMonth: String,
    val latestMonth: String,
    /** Percent change over the trailing 12 months; null when the series is shorter. */
    val change12mPercent: Double?,
)

/** Rows for [series] in the currencies' declaration order. */
fun inflationRows(series: Map<Currency, CpiSeries>): List<InflationRow> = series.entries
    .sortedBy { it.key.ordinal }
    .map { (currency, cpi) ->
        InflationRow(
            currency = currency,
            firstMonth = cpi.firstMonth,
            latestMonth = cpi.latestMonth,
            change12mPercent = cpi.changePercent(12),
        )
    }

/** Formats a percent change with one decimal and an explicit sign: "+1.5%", "−0.3%", "0.0%". */
fun formatPercentChange(percent: Double): String {
    val tenths = round(abs(percent) * 10).toLong()
    val sign = when {
        percent < -0.05 -> "−"
        percent > 0.05 -> "+"
        else -> ""
    }
    return "$sign${tenths / 10}.${tenths % 10}%"
}
