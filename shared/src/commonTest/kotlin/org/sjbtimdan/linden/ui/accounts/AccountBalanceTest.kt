package org.sjbtimdan.linden.ui.accounts

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.math.roundToLong
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate

class AccountBalanceTest : StringSpec({
    val main = Account(id = 1, name = "Main", currency = Currency.CHF, initialBalance = 10_000)
    val savings = Account(id = 2, name = "Savings", currency = Currency.CHF)
    val euros = Account(id = 3, name = "Euros", currency = Currency.EUR, initialBalance = 5_000)

    "returns the initial balances when there are no deltas" {
        accountBalancesMinor(emptyMap(), listOf(main, savings)) shouldBe mapOf(
            1L to 10_000L,
            2L to 0L,
        )
    }

    "adds income and subtracts expenses" {
        val deltas = mapOf(1L to 49_550L)

        accountBalancesMinor(deltas, listOf(main)) shouldBe mapOf(1L to 59_550L)
    }

    "adds the transfer-out loss and the received amount to the target" {
        val deltas = mapOf(
            1L to -10_000L,
            3L to 9_500L,
        )

        accountBalancesMinor(deltas, listOf(main, euros)) shouldBe mapOf(
            1L to 0L,
            3L to 14_500L,
        )
    }

    "accounts without matching deltas keep their initial balance" {
        val deltas = mapOf(2L to -450L)

        accountBalancesMinor(deltas, listOf(main, savings)) shouldBe mapOf(
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
