package org.sjbtimdan.linden.ui.insights

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.entry.DateLanguage
import kotlin.time.Instant

private val defaultWindowEnd = LocalDate(2026, 8, 1)
private val defaultCurrentMonth = LocalDate(2026, 8, 1)
private val main = Account(1, "Main", Currency.CHF)
private val groceries = Category(1, "Groceries", CategoryType.Expense)
private val salary = Category(2, "Salary", CategoryType.Income)

private fun expense(
    amount: Long,
    at: String,
    account: Account = main,
    category: Category = groceries,
    zone: TimeZone = TimeZone.UTC,
) = ExpenseEntry(
    id = 0,
    category = category,
    description = null,
    account = account,
    amount = amount,
    createdAt = Instant.parse(at),
    createdZone = zone,
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

private fun transfer(amount: Long, at: String, to: Account) = TransferEntry(
    id = 0,
    category = null,
    description = null,
    account = main,
    amount = amount,
    toAccount = to,
    toAmount = null,
    createdAt = Instant.parse(at),
    createdZone = TimeZone.UTC,
)

private fun totals(
    entries: List<org.sjbtimdan.linden.model.Entry>,
    windowEnd: LocalDate = defaultWindowEnd,
    currentMonth: LocalDate = defaultCurrentMonth,
    currency: Currency = Currency.CHF,
    rates: List<FxRate> = emptyList(),
) = monthlyTotals(entries, windowEnd, currentMonth, currency, rates)

class MonthlyTotalsTest : StringSpec({

    "empty entries produce twelve zero months ending at the window end" {
        val totals = totals(emptyList())

        totals shouldHaveSize 12
        totals.first().year shouldBe 2025
        totals.first().monthNumber shouldBe 9
        totals.last().year shouldBe 2026
        totals.last().monthNumber shouldBe 8
        totals.all { it.expenseMinor == 0L && it.incomeMinor == 0L } shouldBe true
    }

    "marks only the real current month" {
        val totals = totals(emptyList(), currentMonth = LocalDate(2026, 3, 1))

        totals.last().isCurrent shouldBe false
        totals.first { it.year == 2026 && it.monthNumber == 3 }.isCurrent shouldBe true
        totals.count { it.isCurrent } shouldBe 1
    }

    "buckets expenses by calendar month in ascending order" {
        val totals = totals(
            listOf(
                expense(450, "2026-08-10T12:00:00Z"),
                expense(150, "2026-08-31T23:59:00Z"),
                expense(200, "2026-07-05T08:00:00Z"),
                expense(600, "2025-09-15T12:00:00Z"),
            ),
        )

        totals.first { it.year == 2026 && it.monthNumber == 7 }.expenseMinor shouldBe 200
        totals.last().expenseMinor shouldBe 600
        totals.first().expenseMinor shouldBe 600
        totals.first().isCurrent shouldBe false
        totals.last().isCurrent shouldBe true
    }

    "drops entries outside the twelve-month window" {
        val totals = totals(
            listOf(expense(999, "2025-08-31T12:00:00Z"), expense(100, "2026-09-01T00:00:00Z")),
        )

        totals.all { it.expenseMinor == 0L } shouldBe true
    }

    "totals income separately from expenses" {
        val totals = totals(
            listOf(
                expense(450, "2026-08-10T12:00:00Z"),
                income(5_000, "2026-08-01T09:00:00Z"),
                income(2_000, "2026-07-03T09:00:00Z"),
            ),
        )

        totals.last().expenseMinor shouldBe 450
        totals.last().incomeMinor shouldBe 5_000
        totals.first { it.year == 2026 && it.monthNumber == 7 }.incomeMinor shouldBe 2_000
    }

    "ignores transfers" {
        val totals = totals(
            listOf(transfer(1_000, "2026-08-02T09:00:00Z", to = Account(2, "Savings", Currency.CHF))),
        )

        totals.last().expenseMinor shouldBe 0
        totals.last().incomeMinor shouldBe 0
    }

    "buckets by the entry's own time zone" {
        val totals = totals(
            listOf(
                // 23:00 UTC on July 31 is already August 1 in Tokyo.
                expense(300, "2026-07-31T23:00:00Z", zone = TimeZone.of("Asia/Tokyo")),
                // 01:30 UTC on August 1 is still July 31 in Los Angeles.
                expense(700, "2026-08-01T01:30:00Z", zone = TimeZone.of("America/Los_Angeles")),
            ),
        )

        totals.first { it.year == 2026 && it.monthNumber == 7 }.expenseMinor shouldBe 700
        totals.first { it.year == 2026 && it.monthNumber == 8 }.expenseMinor shouldBe 300
    }

    "converts foreign-currency expenses into the default currency" {
        val usd = Account(3, "US card", Currency.USD)
        val totals = totals(
            listOf(
                expense(1_130, "2026-08-03T12:00:00Z", account = usd),
                expense(200, "2026-08-04T12:00:00Z"),
            ),
            rates = listOf(FxRate(Currency.CHF, Currency.USD, 1.13, "2026-08-01")),
        )

        totals.last().expenseMinor shouldBe 1_200
    }

    "marks a series incomplete when its currency has no stored rate" {
        val usd = Account(3, "US card", Currency.USD)
        val totals = totals(
            listOf(
                expense(1_130, "2026-08-03T12:00:00Z", account = usd),
                income(5_000, "2026-08-01T09:00:00Z"),
                expense(200, "2026-07-04T12:00:00Z"),
            ),
        )

        totals.first { it.year == 2026 && it.monthNumber == 7 }.expenseMinor shouldBe 200
        totals.last().incomeMinor shouldBe 5_000
        totals.last().expenseMinor.shouldBeNull()
    }

    "deltaToPrevious compares expense totals" {
        val august = MonthTotal(2026, 8, expenseMinor = 1_200, incomeMinor = 5_000, isCurrent = true)
        val july = MonthTotal(2026, 7, expenseMinor = 1_000, incomeMinor = 5_000, isCurrent = false)

        deltaToPrevious(august, july) shouldBe 200
        deltaToPrevious(july, august) shouldBe -200
    }

    "deltaToPrevious is null without a usable previous month" {
        val august = MonthTotal(2026, 8, expenseMinor = 1_200, incomeMinor = null, isCurrent = true)
        val incomplete = MonthTotal(2026, 7, expenseMinor = null, incomeMinor = 5_000, isCurrent = false)

        deltaToPrevious(august, null).shouldBeNull()
        deltaToPrevious(august, incomplete).shouldBeNull()
        deltaToPrevious(null, august).shouldBeNull()
    }

    "trend bars map labels, both series, unknown values to zero, and the current flag" {
        val months = listOf(
            MonthTotal(2026, 7, expenseMinor = 1_000, incomeMinor = 500, isCurrent = false),
            MonthTotal(2026, 8, expenseMinor = null, incomeMinor = 5_000, isCurrent = true),
        )

        val bars = monthlyTrendBars(months, DateLanguage.English)

        bars shouldHaveSize 2
        bars[0].label shouldBe "Jul"
        bars[0].values shouldBe listOf(1_000L, 500L)
        bars[0].isCurrent shouldBe false
        bars[1].label shouldBe "Aug"
        bars[1].values shouldBe listOf(0L, 5_000L)
        bars[1].isCurrent shouldBe true
    }

    "month indexes advance by month and year" {
        monthIndexOf(2026, 1) shouldBe 2026 * 12
        monthIndexOf(2026, 8) shouldBe 2026 * 12 + 7
        monthIndexOf(LocalDate(2026, 12, 31)) shouldBe 2026 * 12 + 11
        monthIndexOf(LocalDate(2027, 1, 1)) shouldBe 2027 * 12
    }

    "totals are zero rather than incomplete for an empty month" {
        val totals = totals(emptyList())

        totals.last().expenseMinor.shouldNotBeNull()
        totals.last().expenseMinor shouldBe 0
        totals.last().incomeMinor shouldBe 0
    }
})
