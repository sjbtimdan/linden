package org.sjbtimdan.linden.ui.categories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.math.roundToLong
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate

class CategoryBalanceTest : StringSpec({
    val groceries = Category(id = 1, name = "Groceries", type = CategoryType.Expense)
    val salary = Category(id = 2, name = "Salary", type = CategoryType.Income)

    "returns zero when the category has no totals" {
        categoryBalanceMinor(emptyMap(), groceries.id, Currency.CHF, emptyList()) shouldBe 0
    }

    "nets income against expenses of the category" {
        val totals = mapOf((salary.id to Currency.CHF) to 48_800L)

        categoryBalanceMinor(totals, salary.id, Currency.CHF, emptyList()) shouldBe 48_800
    }

    "ignores totals of other categories" {
        val totals = mapOf(
            (groceries.id to Currency.CHF) to -450L,
            (salary.id to Currency.CHF) to -1_200L,
        )

        categoryBalanceMinor(totals, groceries.id, Currency.CHF, emptyList()) shouldBe -450
    }

    "converts foreign totals to the default currency" {
        val totals = mapOf((salary.id to Currency.EUR) to 5_000L)
        val rates = listOf(
            FxRate(baseCurrency = Currency.CHF, quoteCurrency = Currency.EUR, rate = 1.1, date = "2026-08-16"),
        )

        categoryBalanceMinor(totals, salary.id, Currency.CHF, rates) shouldBe (5_000.0 / 1.1).roundToLong()
    }

    "is null when a foreign total has no stored rate" {
        val totals = mapOf((salary.id to Currency.EUR) to 5_000L)

        categoryBalanceMinor(totals, salary.id, Currency.CHF, emptyList()) shouldBe null
    }
})
