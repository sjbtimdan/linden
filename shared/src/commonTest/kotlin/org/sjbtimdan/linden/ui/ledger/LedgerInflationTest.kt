package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.inflation.CpiSeries
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.withLedgerViewModel
import kotlin.time.Instant

/**
 * The ledger's "In today's money" toggle: totals, category totals and row
 * overrides restate entries through the stored CPI series, and switching the
 * toggle off restores the nominal amounts.
 */
@OptIn(ExperimentalTestApi::class)
class LedgerInflationTest : StringSpec({

    val chfSeries = CpiSeries("2020-01", intArrayOf(10_000, 10_000, 10_000, 10_000, 10_000, 10_000, 11_000))

    "the toggle restates totals and row overrides, and switching it off restores nominal" {
        withLedgerViewModel(
            clock = FakeClock(),
            cpiSeries = mapOf(Currency.CHF to chfSeries),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (account, category) = seedLedger(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(
                    id = 0,
                    category = category,
                    description = "Coffee",
                    account = account,
                    amount = 10_000,
                    createdAt = Instant.parse("2020-06-15T12:00:00Z"),
                ),
            )
            val entry = entryDao.getAll().first().single()

            viewModel.inflationAvailable.first { it } shouldBe true
            viewModel.inflationThroughMonth.first { it != null } shouldBe "2020-07"
            viewModel.totalMinor.first { it == -10_000L } shouldBe -10_000

            viewModel.setInflationAdjusted(true)

            viewModel.totalMinor.first { it == -11_000L } shouldBe -11_000
            viewModel.inflationAmountOverrides.first { it.isNotEmpty() }[entry.id] shouldBe 11_000
            viewModel.categoryTotals.first { it.isNotEmpty() }.single().total shouldBe -11_000

            viewModel.setInflationAdjusted(false)

            viewModel.totalMinor.first { it == -10_000L } shouldBe -10_000
            viewModel.inflationAmountOverrides.first { it.isEmpty() } shouldBe emptyMap<Long, Long>()
        }
    }

    "the toggle restates period-end account balances from the period end" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2021, 6, 15)),
            cpiSeries = mapOf(
                Currency.CHF to CpiSeries("2021-01", intArrayOf(10_000, 10_000, 10_000, 10_000, 10_000, 11_000)),
            ),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (account, category) = seedLedger(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(
                    id = 0,
                    category = category,
                    description = "Coffee",
                    account = account,
                    amount = 10_000,
                    createdAt = Instant.parse("2020-06-15T12:00:00Z"),
                ),
            )
            entryDao.getAll().first().single()

            // A past month: the May 2021 balance is restated through May's CPI.
            viewModel.setPeriod(LedgerPeriod.Month)
            viewModel.goToPreviousPeriod()
            viewModel.accountBalancesAtPeriodEnd.first { it.singleOrNull()?.balance == -10_000L }

            viewModel.setInflationAdjusted(true)

            viewModel.accountBalancesAtPeriodEnd.first { it.singleOrNull()?.balance == -11_000L }
                .single().balance shouldBe -11_000
            viewModel.accountTotalAtPeriodEnd.first { it == -11_000L } shouldBe -11_000
            viewModel.accountConversionsAtPeriodEnd.first { it.values.singleOrNull() == -11_000L }
                .values.single() shouldBe -11_000

            viewModel.setInflationAdjusted(false)

            viewModel.accountBalancesAtPeriodEnd.first { it.singleOrNull()?.balance == -10_000L }
                .single().balance shouldBe -10_000
        }
    }

    "entries in a currency without stored CPI stay nominal" {
        withLedgerViewModel(
            clock = FakeClock(),
            defaultCurrency = Currency.JPY,
            cpiSeries = mapOf(Currency.CHF to chfSeries),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Yen", Currency.JPY, initialBalance = 0)
            categoryDao.create("Groceries", CategoryType.Expense)
            val account = accountDao.getAll().first().first { it.name == "Yen" }
            val category = categoryDao.getAll().first().first { it.name == "Groceries" }
            viewModel.createEntry(
                ExpenseEntry(
                    id = 0,
                    category = category,
                    description = "Ramen",
                    account = account,
                    amount = 500,
                    createdAt = Instant.parse("2020-06-15T12:00:00Z"),
                ),
            )
            entryDao.getAll().first().single()

            viewModel.setInflationAdjusted(true)

            viewModel.totalMinor.first { it == -500L } shouldBe -500
        }
    }
})

private suspend fun seedLedger(accountDao: AccountDao, categoryDao: CategoryDao): Pair<Account, Category> {
    accountDao.create("Main", Currency.CHF, initialBalance = 0)
    categoryDao.create("Groceries", CategoryType.Expense)
    val account = accountDao.getAll().first().first { it.name == "Main" }
    val category = categoryDao.getAll().first().first { it.name == "Groceries" }
    return account to category
}
