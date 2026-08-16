package org.sjbtimdan.linden.ui.categories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.math.roundToLong
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry

class CategoryBalanceTest : StringSpec({
    val groceries = Category(id = 1, name = "Groceries", type = CategoryType.Expense)
    val salary = Category(id = 2, name = "Salary", type = CategoryType.Income)
    val main = Account(id = 1, name = "Main", currency = Currency.CHF)
    val euros = Account(id = 2, name = "Euros", currency = Currency.EUR)
    val savings = Account(id = 3, name = "Savings", currency = Currency.CHF)

    "returns zero when the category has no entries" {
        categoryBalanceMinor(emptyList(), groceries, Currency.CHF, emptyList()) shouldBe 0
    }

    "nets income against expenses of the category" {
        val entries = listOf(
            IncomeEntry(id = 1, category = salary, description = "Pay", account = main, amount = 50_000),
            ExpenseEntry(id = 2, category = salary, description = "Refund", account = main, amount = 1_200),
        )

        categoryBalanceMinor(entries, salary, Currency.CHF, emptyList()) shouldBe 48_800
    }

    "ignores entries of other categories" {
        val entries = listOf(
            ExpenseEntry(id = 1, category = groceries, description = "Coffee", account = main, amount = 450),
            ExpenseEntry(id = 2, category = salary, description = "Lunch", account = main, amount = 1_200),
        )

        categoryBalanceMinor(entries, groceries, Currency.CHF, emptyList()) shouldBe -450
    }

    "ignores transfers even when they carry the category" {
        val entries = listOf(
            ExpenseEntry(id = 1, category = groceries, description = "Coffee", account = main, amount = 450),
            TransferEntry(
                id = 2,
                category = groceries,
                description = null,
                account = main,
                amount = 10_000,
                toAccount = savings,
                toAmount = null,
            ),
        )

        categoryBalanceMinor(entries, groceries, Currency.CHF, emptyList()) shouldBe -450
    }

    "converts foreign entries to the default currency" {
        val entries = listOf(
            IncomeEntry(id = 1, category = salary, description = "Pay", account = euros, amount = 5_000),
        )
        val rates = listOf(
            FxRate(baseCurrency = Currency.CHF, quoteCurrency = Currency.EUR, rate = 1.1, date = "2026-08-16"),
        )

        categoryBalanceMinor(entries, salary, Currency.CHF, rates) shouldBe (5_000.0 / 1.1).roundToLong()
    }

    "is null when a foreign entry has no stored rate" {
        val entries = listOf(
            IncomeEntry(id = 1, category = salary, description = "Pay", account = euros, amount = 5_000),
        )

        categoryBalanceMinor(entries, salary, Currency.CHF, emptyList()) shouldBe null
    }
})
