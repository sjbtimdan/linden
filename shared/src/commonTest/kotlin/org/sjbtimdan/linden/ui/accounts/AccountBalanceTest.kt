package org.sjbtimdan.linden.ui.accounts

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

class AccountBalanceTest : StringSpec({
    val main = Account(id = 1, name = "Main", currency = Currency.CHF, initialBalance = 10_000)
    val savings = Account(id = 2, name = "Savings", currency = Currency.CHF)
    val euros = Account(id = 3, name = "Euros", currency = Currency.EUR, initialBalance = 5_000)
    val groceries = Category(id = 1, name = "Groceries", type = CategoryType.Expense)
    val salary = Category(id = 2, name = "Salary", type = CategoryType.Income)

    "returns the initial balances when there are no entries" {
        accountBalancesMinor(emptyList(), listOf(main, savings)) shouldBe mapOf(
            1L to 10_000L,
            2L to 0L,
        )
    }

    "adds income and subtracts expenses" {
        val entries = listOf(
            IncomeEntry(id = 1, category = salary, description = "Pay", account = main, amount = 50_000),
            ExpenseEntry(id = 2, category = groceries, description = "Coffee", account = main, amount = 450),
        )

        accountBalancesMinor(entries, listOf(main)) shouldBe mapOf(1L to 59_550L)
    }

    "subtracts transfer-out and adds the received amount to the target" {
        val entries = listOf(
            TransferEntry(id = 1, category = null, description = null, account = main, amount = 10_000, toAccount = euros, toAmount = 9_500),
        )

        accountBalancesMinor(entries, listOf(main, euros)) shouldBe mapOf(
            1L to 0L,
            3L to 14_500L,
        )
    }

    "same-currency transfers credit the sent amount to the target" {
        val entries = listOf(
            TransferEntry(id = 1, category = null, description = null, account = main, amount = 10_000, toAccount = savings, toAmount = null),
        )

        accountBalancesMinor(entries, listOf(main, savings)) shouldBe mapOf(
            1L to 0L,
            2L to 10_000L,
        )
    }

    "accounts without matching entries keep their initial balance" {
        val entries = listOf(
            ExpenseEntry(id = 1, category = groceries, description = "Coffee", account = savings, amount = 450),
        )

        accountBalancesMinor(entries, listOf(main, savings)) shouldBe mapOf(
            1L to 10_000L,
            2L to -450L,
        )
    }

    "total sums same-currency balances" {
        val items = listOf(
            AccountWithBalance(main, 10_000),
            AccountWithBalance(savings, 5_000),
        )

        accountTotalMinor(items, Currency.CHF, emptyList()) shouldBe 15_000
    }

    "total converts foreign balances to the default currency" {
        val items = listOf(
            AccountWithBalance(main, 10_000),
            AccountWithBalance(euros, 5_000),
        )
        val rates = listOf(
            FxRate(baseCurrency = Currency.CHF, quoteCurrency = Currency.EUR, rate = 1.1, date = "2026-08-16"),
        )

        accountTotalMinor(items, Currency.CHF, rates) shouldBe 10_000 + (5_000.0 / 1.1).roundToLong()
    }

    "total includes negative balances" {
        val items = listOf(
            AccountWithBalance(main, 10_000),
            AccountWithBalance(savings, -1_200),
        )

        accountTotalMinor(items, Currency.CHF, emptyList()) shouldBe 8_800
    }

    "total is null when a foreign balance has no rate" {
        val items = listOf(
            AccountWithBalance(euros, 5_000),
        )

        accountTotalMinor(items, Currency.CHF, emptyList()) shouldBe null
    }

    "total of no accounts is zero" {
        accountTotalMinor(emptyList(), Currency.CHF, emptyList()) shouldBe 0
    }
})
