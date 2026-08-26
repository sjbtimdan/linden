package org.sjbtimdan.linden.ui.rates

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Currency

class RateFormatTest : StringSpec({
    "formatRate rounds to 4 decimals" {
        formatRate(0.93737) shouldBe "0.9374"
        formatRate(123.456789) shouldBe "123.4568"
        formatRate(1.00005) shouldBe "1.0001"
    }

    "formatRate trims trailing zeros" {
        formatRate(1.0) shouldBe "1"
        formatRate(2.5) shouldBe "2.5"
        formatRate(0.5) shouldBe "0.5"
        formatRate(1.0669) shouldBe "1.0669"
    }

    "parseRate accepts dot and comma decimals" {
        parseRate("1.5") shouldBe 1.5
        parseRate("1,5") shouldBe 1.5
        parseRate("1") shouldBe 1.0
        parseRate("0.0001") shouldBe 0.0001
    }

    "parseRate trims surrounding whitespace" {
        parseRate(" 1.5 ") shouldBe 1.5
        parseRate("   ") shouldBe null
    }

    "parseRate rejects zero, negative and non-numeric input" {
        parseRate("0") shouldBe null
        parseRate("0.0") shouldBe null
        parseRate("-1") shouldBe null
        parseRate("abc") shouldBe null
        parseRate("") shouldBe null
        parseRate("1.2.3") shouldBe null
    }

    "parseRate rejects NaN and infinite input" {
        parseRate("NaN") shouldBe null
        parseRate("Infinity") shouldBe null
    }

    "rateRowLabel shows the rate for the quote currency" {
        rateRowLabel(Currency.CHF, Currency.EUR, 1.0669) shouldBe "1 CHF = 1.0669 €"
        rateRowLabel(Currency.USD, Currency.JPY, 150.25) shouldBe "1 $ = 150.25 ¥"
    }

    "rateRowLabel shows a dash when the rate is unset" {
        rateRowLabel(Currency.CHF, Currency.EUR, null) shouldBe "1 CHF = —"
        rateRowLabel(Currency.USD, Currency.JPY, null) shouldBe "1 $ = —"
    }
})
