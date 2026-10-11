package org.sjbtimdan.linden.ui.insights

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.inflation.CpiSeries
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.withInsightsViewModel
import kotlin.time.Instant

/**
 * The insights screen's "In today's money" toggle: the 12-month trend and the
 * selected month's category breakdown restate through the stored CPI series,
 * and switching the toggle off restores the nominal amounts.
 */
@OptIn(ExperimentalTestApi::class)
class InsightsInflationTest : StringSpec({

    val chfSeries = CpiSeries("2021-01", intArrayOf(10_000, 10_000, 10_000, 10_000, 10_000, 11_000))

    "the toggle restates the trend and the selected month's breakdown" {
        withInsightsViewModel(
            clock = FakeClock(today = LocalDate(2021, 6, 15)),
            cpiSeries = mapOf(Currency.CHF to chfSeries),
        ) { accountDao, categoryDao, entryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 0)
            categoryDao.create("Groceries", CategoryType.Expense)
            val account = accountDao.getAll().first().first { it.name == "Main" }
            val category = categoryDao.getAll().first().first { it.name == "Groceries" }
            entryDao.create(
                ExpenseEntry(
                    id = 0,
                    category = category,
                    description = null,
                    account = account,
                    amount = 10_000,
                    createdAt = Instant.parse("2021-03-15T12:00:00Z"),
                ),
            )

            // March 2021 is index 8 of the window ending with June 2021.
            viewModel.inflationAvailable.first { it } shouldBe true
            viewModel.inflationThroughMonth.first { it != null } shouldBe "2021-06"
            viewModel.months.first { it.getOrNull(8)?.expenseMinor == 10_000L }[8].monthNumber shouldBe 3

            viewModel.selectMonth(8)
            viewModel.breakdown.first { it?.expenses?.singleOrNull()?.amountMinor == 10_000L }

            viewModel.setInflationAdjusted(true)

            viewModel.months.first { it.getOrNull(8)?.expenseMinor == 11_000L }[8].expenseMinor shouldBe 11_000
            viewModel.breakdown.first { it?.expenses?.singleOrNull()?.amountMinor == 11_000L }
                ?.expenses?.single()?.amountMinor shouldBe 11_000

            viewModel.setInflationAdjusted(false)

            viewModel.months.first { it.getOrNull(8)?.expenseMinor == 10_000L }[8].expenseMinor shouldBe 10_000
            viewModel.breakdown.first { it?.expenses?.singleOrNull()?.amountMinor == 10_000L }
                ?.expenses?.single()?.amountMinor shouldBe 10_000
        }
    }
})
