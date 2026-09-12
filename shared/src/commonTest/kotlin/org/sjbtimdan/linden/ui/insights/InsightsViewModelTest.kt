package org.sjbtimdan.linden.ui.insights

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRateDao
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.onTestMain
import org.sjbtimdan.linden.ui.testAllEntries
import org.sjbtimdan.linden.ui.testRatesProvider
import kotlin.time.Instant

private class InsightsFixture(
    val database: LindenDatabase,
    val accountDao: AccountDao,
    val categoryDao: CategoryDao,
    val entryDao: EntryDao,
    val budgetDao: BudgetDao,
    val viewModel: InsightsViewModel,
) {
    suspend fun account(name: String = "Main", currency: Currency = Currency.CHF): Account {
        accountDao.create(name, currency)
        return accountDao.getAll().first().single()
    }

    suspend fun category(name: String = "Groceries"): Category {
        categoryDao.create(name, CategoryType.Expense)
        return categoryDao.getAll().first().first { it.name == name }
    }

    suspend fun expense(category: Category, account: Account, amount: Long, at: String) {
        entryDao.create(
            ExpenseEntry(
                id = 0,
                category = category,
                description = null,
                account = account,
                amount = amount,
                createdAt = Instant.parse(at),
            ),
        )
    }

    suspend fun income(category: Category, account: Account, amount: Long, at: String) {
        entryDao.create(
            IncomeEntry(
                id = 0,
                category = category,
                description = null,
                account = account,
                amount = amount,
                createdAt = Instant.parse(at),
            ),
        )
    }
}

private suspend fun insightsFixture(): InsightsFixture {
    val database = lindenDatabase()
    val accountDao = AccountDao(database.accountQueries)
    val categoryDao = CategoryDao(database.categoryQueries)
    val entryDao = EntryDao(database.entryQueries)
    val settingsDao = SettingsDao(database.settingsQueries)
    val budgetDao = BudgetDao(database.budgetQueries)
    val viewModel = InsightsViewModel(
        entryDao,
        settingsDao,
        testRatesProvider(settingsDao, FxRateDao(database.fxRateQueries)),
        budgetDao,
        testAllEntries(entryDao),
        clock = FakeClock(today = LocalDate(2026, 8, 15)),
    )
    return InsightsFixture(database, accountDao, categoryDao, entryDao, budgetDao, viewModel)
}

@OptIn(ExperimentalTestApi::class)
class InsightsViewModelTest : StringSpec({

    "months roll twelve zero months ending at the current month with no data" {
        onTestMain {
            runComposeUiTest {
                val fixture = insightsFixture()

                val months = fixture.viewModel.months.first { it.isNotEmpty() }
                months shouldHaveSize 12
                months.last().year shouldBe 2026
                months.last().monthNumber shouldBe 8
                months.last().isCurrent shouldBe true
                months.all { it.expenseMinor == 0L && it.incomeMinor == 0L } shouldBe true
                fixture.viewModel.canStepForward.value shouldBe false
                fixture.viewModel.hasEntries.value shouldBe false
            }
        }
    }

    "months roll twelve expense and income totals ending at the window end" {
        onTestMain {
            runComposeUiTest {
                val fixture = insightsFixture()
                val main = fixture.account()
                val groceries = fixture.category()
                fixture.viewModel.months.first { it.isNotEmpty() }

                fixture.expense(groceries, main, 450, "2026-08-10T12:00:00Z")
                fixture.expense(groceries, main, 250, "2026-06-01T12:00:00Z")
                fixture.income(groceries, main, 5_000, "2026-08-01T09:00:00Z")

                fixture.viewModel.hasEntries.first { it } shouldBe true
                val months = fixture.viewModel.months.first { it.last().expenseMinor == 450L }
                months shouldHaveSize 12
                months.last().year shouldBe 2026
                months.last().monthNumber shouldBe 8
                months.last().isCurrent shouldBe true
                months.last().expenseMinor shouldBe 450
                months.last().incomeMinor shouldBe 5_000
                months.first { it.year == 2026 && it.monthNumber == 6 }.expenseMinor shouldBe 250
            }
        }
    }

    "months follow new entries reactively" {
        onTestMain {
            runComposeUiTest {
                val fixture = insightsFixture()
                val main = fixture.account()
                val groceries = fixture.category()
                fixture.viewModel.months.first { it.isNotEmpty() }

                fixture.expense(groceries, main, 450, "2026-08-10T12:00:00Z")

                fixture.viewModel.months.first { it.last().expenseMinor == 450L }
                    .last().expenseMinor shouldBe 450
            }
        }
    }

    "stepBack pages the window twelve months backwards and stepForward returns" {
        onTestMain {
            runComposeUiTest {
                val fixture = insightsFixture()
                val main = fixture.account()
                val groceries = fixture.category()
                fixture.viewModel.months.first { it.isNotEmpty() }

                // May 2025 is outside the initial window (Sep 2025 - Aug 2026).
                fixture.expense(groceries, main, 500, "2025-05-10T12:00:00Z")
                fixture.viewModel.months.first { it.isNotEmpty() }
                fixture.viewModel.months.value.none { it.year == 2025 && it.monthNumber == 5 } shouldBe true

                fixture.viewModel.stepBack()
                fixture.viewModel.windowEnd.first { it == LocalDate(2025, 8, 1) }
                fixture.viewModel.canStepForward.value shouldBe true

                val window = fixture.viewModel.months.first { months ->
                    months.any { it.year == 2025 && it.monthNumber == 5 && it.expenseMinor == 500L }
                }
                window.last().year shouldBe 2025
                window.last().monthNumber shouldBe 8
                window.last().isCurrent shouldBe false
                window.first { it.year == 2025 && it.monthNumber == 5 }.expenseMinor shouldBe 500

                fixture.viewModel.stepForward()
                fixture.viewModel.windowEnd.first { it == LocalDate(2026, 8, 1) }
                fixture.viewModel.canStepForward.value shouldBe false
                fixture.viewModel.months.first { it.isNotEmpty() }.last().expenseMinor shouldBe 0
            }
        }
    }

    "stepForward never pages past the current month" {
        onTestMain {
            runComposeUiTest {
                val fixture = insightsFixture()

                fixture.viewModel.stepForward()
                fixture.viewModel.windowEnd.value shouldBe LocalDate(2026, 8, 1)

                fixture.viewModel.stepBack()
                fixture.viewModel.stepBack()
                fixture.viewModel.windowEnd.value shouldBe LocalDate(2024, 8, 1)

                fixture.viewModel.stepForward()
                fixture.viewModel.windowEnd.value shouldBe LocalDate(2025, 8, 1)
            }
        }
    }

    "breakdown carries the window-end month by default, with budgets attached" {
        onTestMain {
            runComposeUiTest {
                val fixture = insightsFixture()
                val main = fixture.account()
                val groceries = fixture.category("Groceries")
                val salary = fixture.category("Salary")
                fixture.viewModel.months.first { it.isNotEmpty() }

                fixture.expense(groceries, main, 450, "2026-08-10T12:00:00Z")
                fixture.income(salary, main, 5_000, "2026-08-01T09:00:00Z")
                fixture.budgetDao.upsert("Groceries", 800)

                val breakdown = fixture.viewModel.breakdown.first {
                    it?.incomes?.singleOrNull()?.amountMinor == 5_000L &&
                        it.expenses.singleOrNull()?.budgetMinor == 800L
                }!!
                breakdown.expenses.single().category shouldBe groceries
                breakdown.expenses.single().amountMinor shouldBe 450
                breakdown.incomes.single().category shouldBe salary
            }
        }
    }

    "breakdown follows the selected month and remembers previous-month totals" {
        onTestMain {
            runComposeUiTest {
                val fixture = insightsFixture()
                val main = fixture.account()
                val groceries = fixture.category("Groceries")
                fixture.viewModel.months.first { it.isNotEmpty() }

                fixture.expense(groceries, main, 450, "2026-08-10T12:00:00Z")
                fixture.expense(groceries, main, 100, "2026-07-05T12:00:00Z")
                fixture.expense(groceries, main, 300, "2026-06-05T12:00:00Z")
                fixture.viewModel.breakdown.first {
                    it?.expenses?.singleOrNull()?.amountMinor == 450L &&
                        it.expenses.singleOrNull()?.previousMinor == 100L
                }!!

                // July 2026 is column ten of the Sep 2025 - Aug 2026 window.
                fixture.viewModel.selectMonth(10)

                val july = fixture.viewModel.breakdown.first {
                    it?.expenses?.singleOrNull()?.amountMinor == 100L
                }!!
                july.expenses.single().previousMinor shouldBe 300
                fixture.viewModel.selectedIndex.value shouldBe 10
            }
        }
    }

    "paging resets the selection to the new window's last month" {
        onTestMain {
            runComposeUiTest {
                val fixture = insightsFixture()
                val main = fixture.account()
                val groceries = fixture.category("Groceries")
                fixture.viewModel.months.first { it.isNotEmpty() }
                fixture.expense(groceries, main, 450, "2026-08-10T12:00:00Z")
                fixture.viewModel.breakdown.first {
                    it?.expenses?.singleOrNull()?.amountMinor == 450L
                }!!

                fixture.viewModel.selectMonth(0)
                fixture.viewModel.selectedIndex.value shouldBe 0

                fixture.viewModel.stepBack()
                fixture.viewModel.selectedIndex.value shouldBe -1
                fixture.viewModel.windowEnd.value shouldBe LocalDate(2025, 8, 1)

                val oldestWindow = fixture.viewModel.breakdown.first {
                    it != null && it.expenses.isEmpty() && it.incomes.isEmpty()
                }!!
                oldestWindow.expenses.shouldBeEmpty()
            }
        }
    }
})
