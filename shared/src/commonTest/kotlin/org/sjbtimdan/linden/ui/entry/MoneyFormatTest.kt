package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MoneyFormatTest : StringSpec({
    "parseAmount accepts decimal inputs with either separator" {
        parseAmount("42.50") shouldBe 4_250
        parseAmount("42,5") shouldBe 4_250
        parseAmount("42") shouldBe 4_200
        parseAmount("0.05") shouldBe 5
        parseAmount("0.5") shouldBe 50
        parseAmount(" 12.00 ") shouldBe 1_200
    }

    "parseAmount accepts grouped inputs" {
        parseAmount("1,000.00") shouldBe 100_000
        parseAmount("10,000.00") shouldBe 1_000_000
        parseAmount("1.000,00") shouldBe 100_000
        parseAmount("1 000,00") shouldBe 100_000
        parseAmount("1.000,5") shouldBe 100_050
        parseAmount("1,000") shouldBe 100_000
        parseAmount("1.000") shouldBe 100_000
        parseAmount("1 000") shouldBe 100_000
        parseAmount("12.345") shouldBe 1_234_500
        parseAmount("1,000,000") shouldBe 100_000_000
    }

    "parseAmount rejects invalid input" {
        parseAmount("") shouldBe null
        parseAmount("  ") shouldBe null
        parseAmount(".") shouldBe null
        parseAmount("abc") shouldBe null
        parseAmount("12.3.4") shouldBe null
        parseAmount("-42") shouldBe null
        parseAmount("+42") shouldBe null
        parseAmount("1,00,0") shouldBe null
        parseAmount("1,0000") shouldBe null
        parseAmount("12,3456") shouldBe null
    }

    "parse and format round-trip" {
        listOf(0L, 1L, 50L, 4_250L, 12_345L, 1_000_000L, 99_999_999L).forEach { amount ->
            parseAmount(formatAmount(amount)) shouldBe amount
        }
    }
})
