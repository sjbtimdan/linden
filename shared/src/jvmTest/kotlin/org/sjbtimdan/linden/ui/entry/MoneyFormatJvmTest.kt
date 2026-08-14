package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

class MoneyFormatJvmTest : StringSpec({
    "formatAmount renders minor units with two decimals" {
        formatAmount(0, Locale.US) shouldBe "0.00"
        formatAmount(5, Locale.US) shouldBe "0.05"
        formatAmount(450, Locale.US) shouldBe "4.50"
        formatAmount(5_000, Locale.US) shouldBe "50.00"
        formatAmount(12_345, Locale.US) shouldBe "123.45"
        formatAmount(-450, Locale.US) shouldBe "-4.50"
    }

    "formatAmount groups thousands per locale" {
        formatAmount(1_000_000, Locale.US) shouldBe "10,000.00"
        formatAmount(1_000_000, Locale.GERMANY) shouldBe "10.000,00"
        formatAmount(12_345_678, Locale.US) shouldBe "123,456.78"
        formatAmount(12_345_678, Locale.GERMANY) shouldBe "123.456,78"
    }
})
