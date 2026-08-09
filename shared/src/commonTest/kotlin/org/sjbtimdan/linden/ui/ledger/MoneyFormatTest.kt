package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MoneyFormatTest : StringSpec({
    "formatAmount renders minor units with two decimals" {
        formatAmount(0) shouldBe "0.00"
        formatAmount(5) shouldBe "0.05"
        formatAmount(450) shouldBe "4.50"
        formatAmount(5_000) shouldBe "50.00"
        formatAmount(12_345) shouldBe "123.45"
        formatAmount(-450) shouldBe "-4.50"
    }

    "parseAmount accepts decimal inputs with either separator" {
        parseAmount("42.50") shouldBe 4_250
        parseAmount("42,5") shouldBe 4_250
        parseAmount("42") shouldBe 4_200
        parseAmount("0.05") shouldBe 5
        parseAmount("0.5") shouldBe 50
        parseAmount(" 12.00 ") shouldBe 1_200
    }

    "parseAmount rejects invalid input" {
        parseAmount("") shouldBe null
        parseAmount("  ") shouldBe null
        parseAmount(".") shouldBe null
        parseAmount("abc") shouldBe null
        parseAmount("12.345") shouldBe null
        parseAmount("12.3.4") shouldBe null
        parseAmount("-42") shouldBe null
        parseAmount("+42") shouldBe null
    }

    "parse and format round-trip" {
        listOf(0L, 1L, 50L, 4_250L, 12_345L, 99_999_999L).forEach { amount ->
            parseAmount(formatAmount(amount)) shouldBe amount
        }
    }
})
