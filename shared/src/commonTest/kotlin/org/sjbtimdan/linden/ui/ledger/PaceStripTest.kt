package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.withLedgerViewModel
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class PaceStripTest : StringSpec({

    "shows the current-month pace and expands in place" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seedPace(entryDao, accountDao, categoryDao)
            viewModel.setPeriod(LedgerPeriod.Month)

            setContent { LedgerScreen(viewModel = viewModel) }
            waitForIdle()

            onNodeWithTag("paceBar").assertIsDisplayed()
            onNodeWithText("On pace for 1,500.00 CHF").assertIsDisplayed()
            onNodeWithText("+50% vs usual").assertIsDisplayed()

            onNodeWithTag("paceBar").performClick()
            waitForIdle()

            onNodeWithTag("paceCard").assertIsDisplayed()
            onNodeWithText("Usual: 1,000.00 CHF").assertIsDisplayed()
            onNodeWithText("Likely: 1,500.00 CHF – 1,500.00 CHF").assertIsDisplayed()
            onNodeWithText("41.66 CHF/day left to stay at your usual").assertIsDisplayed()

            onNodeWithContentDescription("Hide pace details").performClick()
            waitForIdle()

            onNodeWithTag("paceBar").assertIsDisplayed()
        }
    }

    "stays hidden outside the current month" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seedPace(entryDao, accountDao, categoryDao)

            setContent { LedgerScreen(viewModel = viewModel) }
            waitForIdle()

            onNodeWithTag("paceBar").assertDoesNotExist()
        }
    }

    "hides with hidden totals" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seedPace(entryDao, accountDao, categoryDao)
            viewModel.setPeriod(LedgerPeriod.Month)
            viewModel.setHideTotal(true)

            setContent { LedgerScreen(viewModel = viewModel) }
            waitForIdle()

            onNodeWithTag("paceBar").assertDoesNotExist()
        }
    }

    "hides while the filter panel is open" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seedPace(entryDao, accountDao, categoryDao)
            viewModel.setPeriod(LedgerPeriod.Month)

            setContent { LedgerScreen(viewModel = viewModel) }
            waitForIdle()

            onNodeWithTag("filtersHeader").performClick()
            waitForIdle()

            onNodeWithTag("paceBar").assertDoesNotExist()
        }
    }

    "shows above an empty current month" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seedPace(entryDao, accountDao, categoryDao, currentMonthAmount = null)
            viewModel.setPeriod(LedgerPeriod.Month)

            setContent { LedgerScreen(viewModel = viewModel) }
            waitForIdle()

            onNodeWithTag("paceBar").assertIsDisplayed()
            onNodeWithText("No entries match.").assertIsDisplayed()
        }
    }
})

private suspend fun seedPace(
    entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
    currentMonthAmount: Long? = 50_000,
) {
    accountDao.create("Main", Currency.CHF)
    categoryDao.create("Food", CategoryType.Expense)
    val account = accountDao.getAll().first().first()
    val category = categoryDao.getAll().first().first()
    listOf(
        "2025-10-25T12:00:00Z",
        "2025-11-25T12:00:00Z",
        "2025-12-25T12:00:00Z",
    ).forEach { at ->
        entryDao.create(
            ExpenseEntry(
                id = 0,
                category = category,
                description = null,
                account = account,
                amount = 100_000,
                createdAt = Instant.parse(at),
                createdZone = TimeZone.UTC,
            ),
        )
    }
    currentMonthAmount?.let { amount ->
        entryDao.create(
            ExpenseEntry(
                id = 0,
                category = category,
                description = null,
                account = account,
                amount = amount,
                createdAt = Instant.parse("2026-01-05T12:00:00Z"),
                createdZone = TimeZone.UTC,
            ),
        )
    }
}
