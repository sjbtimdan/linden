package org.sjbtimdan.linden.ui.inflation

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.CpiDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.inflation.CpiSeries
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.onTestMain

class InflationViewModelTest : StringSpec({

    "rows reflect the stored months" {
        onTestMain {
            val database = lindenDatabase()
            val values = IntArray(13) { 10_000 }
            values[12] = 10_200
            val series = CpiSeries("2024-06", values)
            series.values.forEachIndexed { index, value ->
                database.cpiQueries.insertOrReplace("CHF", series.monthAt(index), value.toLong())
            }

            val viewModel = InflationViewModel(CpiDao(database.cpiQueries))
            val rows = viewModel.rows.first { it.isNotEmpty() }

            rows shouldBe listOf(InflationRow(Currency.CHF, "2024-06", "2025-06", 2.0))
        }
    }
})
