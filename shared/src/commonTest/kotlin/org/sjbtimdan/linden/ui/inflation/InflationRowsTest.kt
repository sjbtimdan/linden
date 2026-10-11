package org.sjbtimdan.linden.ui.inflation

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.inflation.CpiSeries
import org.sjbtimdan.linden.model.Currency

class InflationRowsTest : StringSpec({

    "rows come in currency declaration order with 12-month changes" {
        val values = IntArray(13) { 10_000 }
        values[12] = 10_200
        val chf = CpiSeries("2024-06", values)
        val usd = CpiSeries("2025-01", intArrayOf(20_000))

        val rows = inflationRows(mapOf(Currency.USD to usd, Currency.CHF to chf))

        rows.map { it.currency } shouldBe listOf(Currency.CHF, Currency.USD)
        val chfRow = rows.first()
        chfRow.firstMonth shouldBe "2024-06"
        chfRow.latestMonth shouldBe "2025-06"
        chfRow.change12mPercent shouldBe 2.0
        rows.last().change12mPercent.shouldBeNull()
    }

    "formatPercentChange keeps one decimal with an explicit sign" {
        formatPercentChange(1.54) shouldBe "+1.5%"
        formatPercentChange(-0.34) shouldBe "−0.3%"
        formatPercentChange(0.0) shouldBe "0.0%"
        formatPercentChange(12.04) shouldBe "+12.0%"
    }
})
