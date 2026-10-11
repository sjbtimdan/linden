package org.sjbtimdan.linden.inflation

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.TransferEntry
import kotlin.time.Instant

private val chfSeries = CpiSeries("2020-01", intArrayOf(10_000, 10_000, 10_000, 10_000, 10_000, 10_000, 11_000))
private val usdSeries = CpiSeries("2020-01", intArrayOf(20_000, 20_000, 20_000, 20_000, 20_000, 20_000, 21_000))

private val chfAccount = Account(1, "Main", Currency.CHF, 0)
private val usdAccount = Account(2, "Dollar", Currency.USD, 0)
private val jpyAccount = Account(3, "Yen", Currency.JPY, 0)
private val groceries = Category(1, "Groceries", CategoryType.Expense)

private fun expense(account: Account, amount: Long) = ExpenseEntry(
    id = 1,
    category = groceries,
    description = "Coffee",
    account = account,
    amount = amount,
    createdAt = Instant.parse("2020-06-15T12:00:00Z"),
)

class EntryInflationTest : StringSpec({

    val series = mapOf(Currency.CHF to chfSeries, Currency.USD to usdSeries)

    "a disabled adjustment leaves amounts untouched" {
        val inflation = EntryInflation(enabled = false, series = series, zone = TimeZone.UTC)
        val entry = expense(chfAccount, 10_000)

        inflation.adjustedAmount(entry).shouldBeNull()
        inflation.adjustedEntries(listOf(entry)) shouldBe listOf(entry)
    }

    "enabled restates the amount in the latest month's money" {
        val inflation = EntryInflation(enabled = true, series = series, zone = TimeZone.UTC)
        val entry = expense(chfAccount, 10_000)

        inflation.adjustedAmount(entry) shouldBe 11_000
        val adjusted = inflation.adjustedEntries(listOf(entry)).single()
        adjusted.amount shouldBe 11_000
        adjusted.id shouldBe entry.id
    }

    "a transfer adjusts both sides in their own currencies" {
        val inflation = EntryInflation(enabled = true, series = series, zone = TimeZone.UTC)
        val transfer = TransferEntry(
            id = 2,
            category = null,
            description = null,
            account = chfAccount,
            amount = 10_000,
            createdAt = Instant.parse("2020-06-15T12:00:00Z"),
            toAccount = usdAccount,
            toAmount = 12_000,
        )

        val adjusted = inflation.adjustedEntries(listOf(transfer)).single() as TransferEntry
        adjusted.amount shouldBe 11_000
        adjusted.toAmount shouldBe 12_600
    }

    "a currency without a series passes through unchanged" {
        val inflation = EntryInflation(enabled = true, series = series, zone = TimeZone.UTC)
        val entry = expense(jpyAccount, 500)

        inflation.adjustedAmount(entry).shouldBeNull()
        inflation.adjustedEntries(listOf(entry)).single().amount shouldBe 500
    }

    "entries outside the series range clamp to its ends" {
        val inflation = EntryInflation(enabled = true, series = series, zone = TimeZone.UTC)
        val before = expense(chfAccount, 10_000).copy(createdAt = Instant.parse("2019-01-01T12:00:00Z"))
        val after = expense(chfAccount, 10_000).copy(createdAt = Instant.parse("2021-01-01T12:00:00Z"))

        inflation.adjustedAmount(before) shouldBe 11_000
        inflation.adjustedAmount(after) shouldBe 10_000
    }

    "adjustedBalance restates a period-end balance from its as-of date" {
        val inflation = EntryInflation(enabled = true, series = series, zone = TimeZone.UTC)

        inflation.adjustedBalance(10_000, Currency.CHF, LocalDate(2020, 1, 15)) shouldBe 11_000
        inflation.adjustedBalance(-10_000, Currency.CHF, LocalDate(2020, 1, 15)) shouldBe -11_000
        inflation.adjustedBalance(10_000, Currency.JPY, LocalDate(2020, 1, 15)).shouldBeNull()
    }

    "adjustedBalance is null while disabled" {
        val inflation = EntryInflation(enabled = false, series = series, zone = TimeZone.UTC)

        inflation.adjustedBalance(10_000, Currency.CHF, LocalDate(2020, 1, 15)).shouldBeNull()
    }
})
