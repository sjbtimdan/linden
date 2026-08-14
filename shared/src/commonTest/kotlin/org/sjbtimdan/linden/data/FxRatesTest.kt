package org.sjbtimdan.linden.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRates

class FxRatesTest : StringSpec({
    "parseFxRatesResponse parses a Frankfurter response" {
        val text = """
            {"amount":1.0,"base":"CHF","date":"2026-08-13","rates":{"EUR":1.0669,"USD":1.2306}}
        """.trimIndent()

        parseFxRatesResponse(text) shouldBe FxRates(
            base = Currency.CHF,
            date = "2026-08-13",
            rates = mapOf(
                Currency.EUR to 1.0669,
                Currency.USD to 1.2306,
            ),
        )
    }

    "parseFxRatesResponse ignores unknown fields" {
        val text = """
            {"amount":1.0,"base":"CHF","date":"2026-08-13","rates":{"EUR":1.0669},"extra":"ignored"}
        """.trimIndent()

        parseFxRatesResponse(text).rates shouldBe mapOf(Currency.EUR to 1.0669)
    }

    "parseFxRatesResponse fails on an unknown currency code" {
        val text = """
            {"amount":1.0,"base":"XYZ","date":"2026-08-13","rates":{"EUR":1.0669}}
        """.trimIndent()

        shouldThrow<IllegalStateException> { parseFxRatesResponse(text) }
    }
})
