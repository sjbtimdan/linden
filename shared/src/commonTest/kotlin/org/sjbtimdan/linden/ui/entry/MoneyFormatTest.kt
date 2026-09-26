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

    "parseAmount accepts negative amounts" {
        parseAmount("-42") shouldBe -4_200
        parseAmount("-42.50") shouldBe -4_250
        parseAmount("-1,000.00") shouldBe -100_000
        parseAmount("-0.05") shouldBe -5
        parseAmount("-0") shouldBe 0
        parseAmount(" -500 ") shouldBe -50_000
    }

    "parseAmount accepts a leading plus sign" {
        parseAmount("+42") shouldBe 4_200
        parseAmount("+42.50") shouldBe 4_250
        parseAmount(" +500 ") shouldBe 50_000
    }

    "parseAmount accepts locale separators and digits" {
        parseAmount("1'234.50") shouldBe 123_450
        parseAmount("1’234.50") shouldBe 123_450
        parseAmount("1'234,50") shouldBe 123_450
        parseAmount("1\u00A0234,50") shouldBe 123_450
        parseAmount("١٬٢٣٤٫٥٦") shouldBe 123_456
        parseAmount("۱۲۳٫۴۵") shouldBe 12_345
    }

    "parseAmount rejects invalid input" {
        parseAmount("") shouldBe null
        parseAmount("  ") shouldBe null
        parseAmount(".") shouldBe null
        parseAmount("abc") shouldBe null
        parseAmount("12.3.4") shouldBe null
        parseAmount("+") shouldBe null
        parseAmount("5+2") shouldBe null
        parseAmount("-") shouldBe null
        parseAmount("1,00,0") shouldBe null
        parseAmount("1,0000") shouldBe null
        parseAmount("12,3456") shouldBe null
    }

    "filterAmountInput keeps amount characters and drops the rest" {
        filterAmountInput("abc12.5xyz") shouldBe "12.5"
        filterAmountInput("1,000.00") shouldBe "1,000.00"
        filterAmountInput("1.000,00") shouldBe "1.000,00"
        filterAmountInput("+42") shouldBe "+42"
        filterAmountInput("-42") shouldBe "-42"
        filterAmountInput("1’234.56") shouldBe "1’234.56"
        filterAmountInput("12\u221234") shouldBe "12\u221234"
        filterAmountInput("١٬٢٣٤٫٥٦") shouldBe "١٬٢٣٤٫٥٦"
        filterAmountInput("12٣") shouldBe "12٣"
        filterAmountInput("12€") shouldBe "12"
        filterAmountInput("") shouldBe ""
        filterAmountInput("abc") shouldBe ""
    }

    "parse and format round-trip" {
        listOf(0L, 1L, 50L, 4_250L, 12_345L, 1_000_000L, 99_999_999L, -4_250L, -1_000_000L).forEach { amount ->
            parseAmount(formatAmount(amount)) shouldBe amount
        }
    }

    "formatAmountCompact shortens millions and billions" {
        formatAmountCompact(0) shouldBe "0.00"
        formatAmountCompact(99_999_999) shouldBe "999,999.99"
        formatAmountCompact(100_000_000) shouldBe "1m"
        formatAmountCompact(125_000_000) shouldBe "1.25m"
        formatAmountCompact(1_836_523_700) shouldBe "18.365m"
        formatAmountCompact(199_999_999) shouldBe "2m"
        formatAmountCompact(-1_836_523_700) shouldBe "-18.365m"
        formatAmountCompact(123_456_789_000L) shouldBe "1.235b"
        formatAmountCompact(-1_234_567_890_000L) shouldBe "-12.346b"
    }

    "formatAmountCompact keeps leading zeros of the fractional digits" {
        // 1,059,299.14 must not render as "1.59m".
        formatAmountCompact(105_929_914) shouldBe "1.059m"
        formatAmountCompact(105_000_000) shouldBe "1.05m"
        formatAmountCompact(100_500_000) shouldBe "1.005m"
        formatAmountCompact(100_050_000) shouldBe "1.001m"
        formatAmountCompact(-105_929_914) shouldBe "-1.059m"
    }
})
