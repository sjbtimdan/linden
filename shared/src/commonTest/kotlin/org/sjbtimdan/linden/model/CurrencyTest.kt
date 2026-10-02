package org.sjbtimdan.linden.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CurrencyTest : StringSpec({
    "zero-decimal currencies declare no decimal digits" {
        Currency.IDR.decimalDigits shouldBe 0
        Currency.JPY.decimalDigits shouldBe 0
    }

    "the other currencies keep two decimal digits" {
        listOf(
            Currency.CHF,
            Currency.CNY,
            Currency.EUR,
            Currency.GBP,
            Currency.HKD,
            Currency.INR,
            Currency.SGD,
            Currency.USD,
        )
            .forEach { it.decimalDigits shouldBe 2 }
    }

    "IDR renders with the rupiah symbol" {
        Currency.IDR.symbol shouldBe "Rp"
    }
})
