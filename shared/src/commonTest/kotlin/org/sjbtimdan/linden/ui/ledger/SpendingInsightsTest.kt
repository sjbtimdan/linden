package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import kotlin.time.Instant

class SpendingInsightsTest : StringSpec({
    val main = Account(1, "Main", Currency.CHF)
    val savings = Account(2, "Savings", Currency.CHF)
    val groceries = Category(1, "Groceries", CategoryType.Expense, null)
    val transport = Category(2, "Transport", CategoryType.Expense, null)
    val salary = Category(3, "Salary", CategoryType.Income, null)

    fun expense(category: Category, amount: Long) =
        ExpenseEntry(0, category, null, main, amount, createdAt = Instant.parse("2026-08-10T12:00:00Z"))

    "month windows compare month-to-date with the same day-range last month" {
        val (current, previous) = monthInsightWindows(LocalDate(2026, 9, 15), LocalDate(2026, 9, 15)).shouldNotBeNull()
        current shouldBe InsightWindow(LocalDate(2026, 9, 1), LocalDate(2026, 9, 15))
        previous shouldBe InsightWindow(LocalDate(2026, 8, 1), LocalDate(2026, 8, 15))
    }

    "month windows compare a past month in full" {
        val (current, previous) = monthInsightWindows(LocalDate(2026, 8, 15), LocalDate(2026, 9, 15)).shouldNotBeNull()
        current shouldBe InsightWindow(LocalDate(2026, 8, 1), LocalDate(2026, 8, 31))
        previous shouldBe InsightWindow(LocalDate(2026, 7, 1), LocalDate(2026, 7, 31))
    }

    "month windows clamp the previous month to its own length" {
        val (current, previous) = monthInsightWindows(LocalDate(2026, 3, 31), LocalDate(2026, 3, 31)).shouldNotBeNull()
        current shouldBe InsightWindow(LocalDate(2026, 3, 1), LocalDate(2026, 3, 31))
        previous shouldBe InsightWindow(LocalDate(2026, 2, 1), LocalDate(2026, 2, 28))
    }

    "month windows are null for a month entirely in the future" {
        monthInsightWindows(LocalDate(2026, 10, 15), LocalDate(2026, 9, 15)).shouldBeNull()
    }

    "month windows compare a single day on the first of the month" {
        val (current, previous) = monthInsightWindows(LocalDate(2026, 9, 1), LocalDate(2026, 9, 1)).shouldNotBeNull()
        current shouldBe InsightWindow(LocalDate(2026, 9, 1), LocalDate(2026, 9, 1))
        previous shouldBe InsightWindow(LocalDate(2026, 8, 1), LocalDate(2026, 8, 1))
    }

    "compares current spending to the previous month" {
        val insights = computeSpendingInsights(
            currentEntries = listOf(expense(groceries, 1_200)),
            previousEntries = listOf(expense(groceries, 1_000)),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        ).shouldNotBeNull()
        insights.currentSpent shouldBe 1_200L
        insights.previousSpent shouldBe 1_000L
        insights.changePercent shouldBe 20
    }

    "changePercent is null when the previous month had no spending" {
        val insights = computeSpendingInsights(
            currentEntries = listOf(expense(groceries, 500)),
            previousEntries = emptyList(),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        ).shouldNotBeNull()
        insights.changePercent.shouldBeNull()
    }

    "changePercent is negative when spending decreased" {
        val insights = computeSpendingInsights(
            currentEntries = listOf(expense(groceries, 800)),
            previousEntries = listOf(expense(groceries, 1_000)),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        ).shouldNotBeNull()
        insights.changePercent shouldBe -20
    }

    "income and transfers are excluded from spending" {
        val insights = computeSpendingInsights(
            currentEntries = listOf(
                expense(groceries, 500),
                IncomeEntry(0, salary, null, main, 10_000, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
                TransferEntry(
                    id = 0,
                    category = null,
                    description = null,
                    account = main,
                    amount = 300,
                    createdAt = Instant.parse("2026-08-10T12:00:00Z"),
                    toAccount = savings,
                    toAmount = null,
                ),
            ),
            previousEntries = emptyList(),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        ).shouldNotBeNull()
        insights.currentSpent shouldBe 500L
    }

    "top categories are sorted by amount and limited to three" {
        val insights = computeSpendingInsights(
            currentEntries = listOf(
                expense(groceries, 1_000),
                expense(transport, 500),
                expense(groceries, 300),
            ),
            previousEntries = emptyList(),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        ).shouldNotBeNull()
        insights.topCategories.map { it.category?.id } shouldBe listOf(1L, 2L)
        insights.topCategories[0].amount shouldBe 1_300L
        insights.topCategories[0].sharePercent shouldBe 72
        insights.topCategories[1].sharePercent shouldBe 27
    }

    "missing rate returns null" {
        val usd = Account(3, "USD", Currency.USD)
        val insights = computeSpendingInsights(
            currentEntries = listOf(
                ExpenseEntry(0, groceries, null, usd, 1_000, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
            ),
            previousEntries = emptyList(),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        )
        insights.shouldBeNull()
    }

    "empty entries produce zero spending and no categories" {
        val insights = computeSpendingInsights(
            currentEntries = emptyList(),
            previousEntries = emptyList(),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        ).shouldNotBeNull()
        insights.currentSpent shouldBe 0L
        insights.previousSpent shouldBe 0L
        insights.changePercent.shouldBeNull()
        insights.topCategories.shouldBeEmpty()
    }
})
