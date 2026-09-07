package org.sjbtimdan.linden.ui.insights

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Budget
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import kotlin.time.Instant

private val month = LocalDate(2026, 8, 1)
private val main = Account(1, "Main", Currency.CHF)
private val groceries = Category(1, "Groceries", CategoryType.Expense, CategoryIcon.ShoppingCart)
private val dining = Category(2, "Dining", CategoryType.Expense, CategoryIcon.Restaurant)
private val salary = Category(3, "Salary", CategoryType.Income, CategoryIcon.Savings)

private fun expense(category: Category, amount: Long, at: String, account: Account = main) = ExpenseEntry(
    id = 0,
    category = category,
    description = null,
    account = account,
    amount = amount,
    createdAt = Instant.parse(at),
    createdZone = TimeZone.UTC,
)

private fun income(category: Category, amount: Long, at: String) = IncomeEntry(
    id = 0,
    category = category,
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
    toAccount = Account(9, "Savings", Currency.CHF),
    toAmount = null,
    createdAt = Instant.parse(at),
    createdZone = TimeZone.UTC,
)

class CategoryBreakdownTest : StringSpec({

    "groups expenses of the month by category, ranked by amount" {
        val breakdown = categoryBreakdown(
            listOf(
                expense(groceries, 450, "2026-08-10T12:00:00Z"),
                expense(groceries, 50, "2026-08-20T12:00:00Z"),
                expense(dining, 900, "2026-08-02T12:00:00Z"),
            ),
            month,
            Currency.CHF,
            emptyList(),
            emptyList(),
        )

        breakdown.expenses shouldHaveSize 2
        breakdown.expenses[0].category shouldBe dining
        breakdown.expenses[0].amountMinor shouldBe 900
        breakdown.expenses[0].count shouldBe 1
        breakdown.expenses[1].category shouldBe groceries
        breakdown.expenses[1].amountMinor shouldBe 500
        breakdown.expenses[1].count shouldBe 2
        breakdown.incomes.shouldBeEmpty()
    }

    "keeps income in its own section without budgets" {
        val breakdown = categoryBreakdown(
            listOf(
                expense(groceries, 450, "2026-08-10T12:00:00Z"),
                income(salary, 5_000, "2026-08-01T09:00:00Z"),
            ),
            month,
            Currency.CHF,
            emptyList(),
            listOf(Budget("Groceries", 800)),
        )

        breakdown.expenses.single().budgetMinor shouldBe 800
        breakdown.incomes.single().category shouldBe salary
        breakdown.incomes.single().amountMinor shouldBe 5_000
        breakdown.incomes.single().budgetMinor.shouldBeNull()
    }

    "pairs each category with its previous-month total" {
        val breakdown = categoryBreakdown(
            listOf(
                expense(groceries, 450, "2026-08-10T12:00:00Z"),
                expense(groceries, 100, "2026-07-10T12:00:00Z"),
                expense(dining, 900, "2026-08-02T12:00:00Z"),
            ),
            month,
            Currency.CHF,
            emptyList(),
            emptyList(),
        )

        val groceriesRow = breakdown.expenses.first { it.category == groceries }
        groceriesRow.previousMinor shouldBe 100
        val diningRow = breakdown.expenses.first { it.category == dining }
        diningRow.previousMinor.shouldBeNull()
    }

    "ignores other months and transfers" {
        val breakdown = categoryBreakdown(
            listOf(
                expense(groceries, 450, "2026-06-10T12:00:00Z"),
                expense(groceries, 999, "2026-09-10T12:00:00Z"),
                transfer(500, "2026-08-05T12:00:00Z"),
            ),
            month,
            Currency.CHF,
            emptyList(),
            emptyList(),
        )

        breakdown.expenses.shouldBeEmpty()
        breakdown.incomes.shouldBeEmpty()
    }

    "matches budgets by name case-insensitively" {
        val breakdown = categoryBreakdown(
            listOf(expense(groceries, 450, "2026-08-10T12:00:00Z")),
            month,
            Currency.CHF,
            emptyList(),
            listOf(Budget("groceries", 800), Budget("Salary", 9_999)),
        )

        breakdown.expenses.single().budgetMinor shouldBe 800
    }

    "marks rows incomplete when their currency has no stored rate" {
        val usdAccount = Account(4, "US card", Currency.USD)
        val breakdown = categoryBreakdown(
            listOf(expense(dining, 1_130, "2026-08-02T12:00:00Z", account = usdAccount)),
            month,
            Currency.CHF,
            emptyList(),
            emptyList(),
        )

        breakdown.expenses.single().amountMinor.shouldBeNull()
    }

    "converts foreign-currency entries into the default currency" {
        val usdAccount = Account(4, "US card", Currency.USD)
        val breakdown = categoryBreakdown(
            listOf(expense(dining, 1_130, "2026-08-02T12:00:00Z", account = usdAccount)),
            month,
            Currency.CHF,
            listOf(FxRate(Currency.CHF, Currency.USD, 1.13, "2026-08-01")),
            emptyList(),
        )

        breakdown.expenses.single().amountMinor shouldBe 1_000
    }

    "monthBefore steps whole months across year boundaries" {
        monthBefore(LocalDate(2026, 8, 1), 0) shouldBe LocalDate(2026, 8, 1)
        monthBefore(LocalDate(2026, 8, 1), 1) shouldBe LocalDate(2026, 7, 1)
        monthBefore(LocalDate(2026, 1, 1), 1) shouldBe LocalDate(2025, 12, 1)
        monthBefore(LocalDate(2026, 8, 1), 11) shouldBe LocalDate(2025, 9, 1)
    }
})
