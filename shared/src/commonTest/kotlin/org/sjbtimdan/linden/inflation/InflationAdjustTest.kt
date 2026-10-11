package org.sjbtimdan.linden.inflation

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class InflationAdjustTest : StringSpec({

    "inflationFactorPpm is the ratio of the latest index to the date's index" {
        val series = CpiSeries("2020-01", intArrayOf(10_000, 10_000, 10_000, 11_000))

        inflationFactorPpm(series, LocalDate(2020, 1, 15)) shouldBe 1_100_000L
        inflationFactorPpm(series, LocalDate(2020, 4, 15)) shouldBe 1_000_000L
    }

    "inflationFactorPpm is null without a series" {
        inflationFactorPpm(null, LocalDate(2020, 1, 15)).shouldBeNull()
    }

    "adjustMinor scales minor units and rounds half-up" {
        adjustMinor(10_000, 1_100_000) shouldBe 11_000
        adjustMinor(-10_000, 1_100_000) shouldBe -11_000
        adjustMinor(15, 1_000_000) shouldBe 15
        adjustMinor(1, 1_500_000) shouldBe 2
        adjustMinor(1, 500_000) shouldBe 1
    }
})
