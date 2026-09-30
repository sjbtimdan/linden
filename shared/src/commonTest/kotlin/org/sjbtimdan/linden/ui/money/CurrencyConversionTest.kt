package org.sjbtimdan.linden.ui.money

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate

class CurrencyConversionTest : StringSpec({
    val chfUsdRate = listOf(FxRate(Currency.CHF, Currency.USD, 0.8, "2026-08-13"))

    "sumInDefaultMinor totals same-currency groups without rates" {
        sumInDefaultMinor(
            listOf(Currency.CHF to 450L, Currency.CHF to -1_200L),
            Currency.CHF,
            emptyList(),
        ) shouldBe -750L
    }

    "sumInDefaultMinor converts foreign groups via the stored rate" {
        sumInDefaultMinor(
            listOf(Currency.CHF to 1_000L, Currency.USD to 800L),
            Currency.CHF,
            chfUsdRate,
        ) shouldBe 2_000L
    }

    "sumInDefaultMinor is null when a foreign group has no rate" {
        sumInDefaultMinor(
            listOf(Currency.CHF to 100L, Currency.EUR to 200L),
            Currency.CHF,
            chfUsdRate,
        ) shouldBe null
    }

    "sumInDefaultMinor of no groups is zero" {
        sumInDefaultMinor(emptyList(), Currency.CHF, emptyList()) shouldBe 0L
    }

    "sumInDefaultMinor sums currency maps via the stored rate" {
        sumInDefaultMinor(
            mapOf(Currency.CHF to 1_000L, Currency.USD to 800L),
            Currency.CHF,
            chfUsdRate,
        ) shouldBe 2_000L
    }

    "sumInDefaultMinor is null when a map currency has no rate" {
        sumInDefaultMinor(
            mapOf(Currency.CHF to 100L, Currency.EUR to 200L),
            Currency.CHF,
            chfUsdRate,
        ) shouldBe null
    }

    "sumInDefaultMinor of an empty map is zero" {
        sumInDefaultMinor(emptyMap(), Currency.CHF, emptyList()) shouldBe 0L
    }
})
