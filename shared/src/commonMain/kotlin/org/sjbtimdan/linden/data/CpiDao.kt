package org.sjbtimdan.linden.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.sjbtimdan.linden.CpiQueries
import org.sjbtimdan.linden.inflation.CpiSeries
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.util.asListFlow

class CpiDao(private val queries: CpiQueries) {
    /** All stored months grouped into per-currency monthly series. */
    fun seriesFlow(): Flow<Map<Currency, CpiSeries>> = queries.selectAll().asListFlow().map { rows ->
        rows.groupBy { Currency.fromCode(it.currency) }.mapValues { (_, months) ->
            CpiSeries(
                firstMonth = months.first().month,
                values = IntArray(months.size) { months[it].indexValue.toInt() },
            )
        }
    }
}
