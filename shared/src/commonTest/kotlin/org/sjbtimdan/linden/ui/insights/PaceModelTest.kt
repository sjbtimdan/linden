package org.sjbtimdan.linden.ui.insights

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import kotlin.time.Instant

private val main = Account(1, "Main", Currency.HKD)
private val second = Account(2, "Second", Currency.HKD)
private val usd = Account(3, "USD", Currency.USD)
private val food = Category(1, "Food", CategoryType.Expense)
private val salary = Category(2, "Salary", CategoryType.Income)

private fun expense(amount: Long, at: String, account: Account = main) = ExpenseEntry(
    id = 0,
    category = food,
    description = null,
    account = account,
    amount = amount,
    createdAt = Instant.parse(at),
    createdZone = TimeZone.UTC,
)

private fun income(amount: Long, at: String) = IncomeEntry(
    id = 0,
    category = salary,
    description = null,
    account = main,
    amount = amount,
    createdAt = Instant.parse(at),
    createdZone = TimeZone.UTC,
)

private fun transfer(amount: Long, at: String) = TransferEntry(
    id = 0,
    category = null,
    description = null,
    account = main,
    amount = amount,
    createdAt = Instant.parse(at),
    createdZone = TimeZone.UTC,
    toAccount = second,
    toAmount = null,
)

private fun rate(quote: Currency, value: Double) = FxRate(Currency.HKD, quote, value, "2026-01-01")

private fun forecast(
    entries: List<Entry>,
    today: LocalDate,
    rates: List<FxRate> = emptyList(),
    defaultCurrency: Currency = Currency.HKD,
    zone: TimeZone = TimeZone.UTC,
    historyMonths: Int = PACE_HISTORY_MONTHS,
) = paceForecast(entries, today, defaultCurrency, rates, zone, historyMonths)

class PaceModelTest : StringSpec({

    "returns null until a complete prior month has expenses" {
        forecast(listOf(expense(100, "2026-06-05T12:00:00Z")), LocalDate(2026, 6, 10)) shouldBe null
        forecast(emptyList(), LocalDate(2026, 6, 10)) shouldBe null
    }

    "projects from the median remaining spend of the prior months" {
        val entries = listOf(
            expense(400, "2026-03-05T12:00:00Z"),
            expense(600, "2026-03-20T12:00:00Z"),
            expense(500, "2026-04-01T12:00:00Z"),
            expense(700, "2026-04-25T12:00:00Z"),
            expense(300, "2026-05-03T12:00:00Z"),
            expense(500, "2026-05-28T12:00:00Z"),
            expense(450, "2026-06-02T12:00:00Z"),
        )

        val result = forecast(entries, LocalDate(2026, 6, 10)) ?: error("expected a forecast")

        result.monthStart shouldBe LocalDate(2026, 6, 1)
        result.spentMinor shouldBe 450
        result.projectedMinor shouldBe 1_050
        result.likelyLowMinor shouldBe 970
        result.likelyHighMinor shouldBe 1_130
        result.typicalMonthMinor shouldBe 1_000
        result.daysRemaining shouldBe 21
        result.dailyAllowanceMinor shouldBe 26
        result.historyMonths shouldBe 3
        result.confidence shouldBe PaceConfidence.Medium
    }

    "uses only the last six complete months of history" {
        val entries = listOf(
            expense(10_000, "2026-01-20T12:00:00Z"),
            expense(100, "2026-02-20T12:00:00Z"),
            expense(200, "2026-03-20T12:00:00Z"),
            expense(300, "2026-04-20T12:00:00Z"),
            expense(400, "2026-05-20T12:00:00Z"),
            expense(500, "2026-06-20T12:00:00Z"),
            expense(600, "2026-07-20T12:00:00Z"),
        )

        val six = forecast(entries, LocalDate(2026, 8, 10)) ?: error("expected a forecast")
        val seven = forecast(entries, LocalDate(2026, 8, 10), historyMonths = 7)
            ?: error("expected a forecast")

        six.projectedMinor shouldBe 350
        six.typicalMonthMinor shouldBe 350
        six.confidence shouldBe PaceConfidence.High
        seven.projectedMinor shouldBe 400
    }

    "ignores income and transfers" {
        val expensesOnly = listOf(
            expense(1_000, "2026-03-20T12:00:00Z"),
            expense(1_000, "2026-04-20T12:00:00Z"),
            expense(1_000, "2026-05-20T12:00:00Z"),
            expense(100, "2026-06-05T12:00:00Z"),
        )
        val withOtherTypes = expensesOnly + listOf(
            income(5_000, "2026-06-06T12:00:00Z"),
            transfer(5_000, "2026-06-07T12:00:00Z"),
        )

        val expenses = forecast(expensesOnly, LocalDate(2026, 6, 10)) ?: error("expected a forecast")
        val mixed = forecast(withOtherTypes, LocalDate(2026, 6, 10)) ?: error("expected a forecast")

        expenses.spentMinor shouldBe 100
        expenses.projectedMinor shouldBe 1_100
        mixed.spentMinor shouldBe expenses.spentMinor
        mixed.projectedMinor shouldBe expenses.projectedMinor
    }

    "excludes future-dated expenses in the current month" {
        val entries = listOf(
            expense(1_000, "2026-03-01T12:00:00Z"),
            expense(1_000, "2026-04-01T12:00:00Z"),
            expense(1_000, "2026-05-01T12:00:00Z"),
            expense(100, "2026-06-05T12:00:00Z"),
            expense(999, "2026-06-11T12:00:00Z"),
        )

        val result = forecast(entries, LocalDate(2026, 6, 10)) ?: error("expected a forecast")

        result.spentMinor shouldBe 100
        result.projectedMinor shouldBe 100
        result.typicalMonthMinor shouldBe 1_000
        result.dailyAllowanceMinor shouldBe 42
    }

    "buckets entries by the classification zone" {
        val losAngeles = TimeZone.of("America/Los_Angeles")
        val entries = listOf(
            expense(100, "2026-06-01T00:30:00Z"),
            expense(200, "2026-06-10T12:00:00Z"),
        )

        val result = forecast(entries, LocalDate(2026, 6, 10), zone = losAngeles)
            ?: error("expected a forecast")

        result.spentMinor shouldBe 200
        result.projectedMinor shouldBe 300
        result.historyMonths shouldBe 1
    }

    "forecasts each currency separately before converting" {
        val entries = listOf(
            expense(1_000, "2026-04-20T12:00:00Z"),
            expense(1_000, "2026-05-20T12:00:00Z"),
            expense(1_000, "2026-06-20T12:00:00Z"),
            expense(780, "2026-05-20T12:00:00Z", usd),
            expense(780, "2026-06-20T12:00:00Z", usd),
        )

        val result = forecast(
            entries = entries,
            today = LocalDate(2026, 7, 10),
            rates = listOf(rate(Currency.USD, 7.8)),
        ) ?: error("expected a forecast")

        result.spentMinor shouldBe 0
        result.projectedMinor shouldBe 1_100
        result.typicalMonthMinor shouldBe 1_100
        result.missingCurrencies shouldBe emptySet()
    }

    "leaves out a currency with no stored rate" {
        val entries = listOf(
            expense(1_000, "2026-04-20T12:00:00Z"),
            expense(1_000, "2026-05-20T12:00:00Z"),
            expense(1_000, "2026-06-20T12:00:00Z"),
            expense(780, "2026-05-20T12:00:00Z", usd),
            expense(780, "2026-06-20T12:00:00Z", usd),
        )

        val result = forecast(entries, LocalDate(2026, 7, 10)) ?: error("expected a forecast")

        result.projectedMinor shouldBe 1_000
        result.typicalMonthMinor shouldBe 1_000
        result.missingCurrencies shouldBe setOf(Currency.USD)
    }

    "falls back to a linear pace for a currency with no history" {
        val entries = listOf(
            expense(1_000, "2026-03-01T12:00:00Z"),
            expense(1_000, "2026-04-01T12:00:00Z"),
            expense(1_000, "2026-05-01T12:00:00Z"),
            expense(780, "2026-06-10T12:00:00Z", usd),
        )

        val result = forecast(
            entries = entries,
            today = LocalDate(2026, 6, 10),
            rates = listOf(rate(Currency.USD, 7.8)),
        ) ?: error("expected a forecast")

        result.spentMinor shouldBe 100
        result.projectedMinor shouldBe 300
    }

    "on the last day the projection equals the month-to-date spend" {
        val entries = listOf(
            expense(1_000, "2026-03-01T12:00:00Z"),
            expense(1_000, "2026-04-01T12:00:00Z"),
            expense(1_000, "2026-05-01T12:00:00Z"),
            expense(500, "2026-06-30T12:00:00Z"),
        )

        val result = forecast(entries, LocalDate(2026, 6, 30)) ?: error("expected a forecast")

        result.daysRemaining shouldBe 1
        result.spentMinor shouldBe 500
        result.projectedMinor shouldBe 500
        result.dailyAllowanceMinor shouldBe 500
    }

    "handles a leap-year February" {
        val entries = listOf(
            expense(1_000, "2027-11-01T12:00:00Z"),
            expense(1_000, "2027-12-01T12:00:00Z"),
            expense(1_000, "2028-01-01T12:00:00Z"),
            expense(500, "2028-02-29T12:00:00Z"),
        )

        val result = forecast(entries, LocalDate(2028, 2, 29)) ?: error("expected a forecast")

        result.daysInMonth shouldBe 29
        result.daysRemaining shouldBe 1
        result.projectedMinor shouldBe 500
    }

    "skips complete months with no expense data when counting history" {
        val entries = listOf(
            expense(1_000, "2026-01-20T12:00:00Z"),
            expense(1_000, "2026-03-20T12:00:00Z"),
        )

        val result = forecast(entries, LocalDate(2026, 6, 10)) ?: error("expected a forecast")

        result.historyMonths shouldBe 2
        result.confidence shouldBe PaceConfidence.Low
        result.projectedMinor shouldBe 1_000
    }

    "stops the daily allowance at zero once the usual total is spent" {
        val entries = listOf(
            expense(1_000, "2026-03-01T12:00:00Z"),
            expense(1_000, "2026-04-01T12:00:00Z"),
            expense(1_000, "2026-05-01T12:00:00Z"),
            expense(1_200, "2026-06-05T12:00:00Z"),
        )

        val result = forecast(entries, LocalDate(2026, 6, 10)) ?: error("expected a forecast")

        result.typicalMonthMinor shouldBe 1_000
        result.projectedMinor shouldBe 1_200
        result.dailyAllowanceMinor shouldBe 0
    }
})
