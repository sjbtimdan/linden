package org.sjbtimdan.linden.inflation

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class CpiSeriesTest : StringSpec({

    "monthAt advances and rolls over year boundaries" {
        val series = CpiSeries("2020-03", intArrayOf(1, 2, 3, 4, 5))

        series.monthAt(0) shouldBe "2020-03"
        series.monthAt(4) shouldBe "2020-07"
        series.monthAt(10) shouldBe "2021-01"
        series.monthAt(15) shouldBe "2021-06"
    }

    "latestMonth is the month of the last value" {
        val series = CpiSeries("2020-03", intArrayOf(1, 2, 3))

        series.latestMonth shouldBe "2020-05"
    }

    "indexAt matches the month and clamps outside the covered range" {
        val series = CpiSeries("2020-03", intArrayOf(100, 200, 300, 400))

        series.indexAt(LocalDate(2020, 1, 15)) shouldBe 100
        series.indexAt(LocalDate(2020, 3, 1)) shouldBe 100
        series.indexAt(LocalDate(2020, 5, 31)) shouldBe 300
        series.indexAt(LocalDate(2021, 1, 1)) shouldBe 400
    }

    "changePercent compares against the index n months earlier" {
        val values = IntArray(13) { 10_000 }
        values[12] = 10_200
        val series = CpiSeries("2024-06", values)

        series.changePercent(12) shouldBe 2.0
        series.changePercent(0) shouldBe 0.0
    }

    "changePercent is null when the series is too short" {
        val series = CpiSeries("2024-06", intArrayOf(10_000, 10_100))

        series.changePercent(12).shouldBeNull()
    }
})
