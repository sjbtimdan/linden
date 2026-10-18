package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
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
import org.sjbtimdan.linden.ui.withEntryPoint
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class EntryPaceCaptionTest : StringSpec({

    "shows the pace after an expense amount is entered" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seedPaceHistory(entryDao, accountDao, categoryDao)

            setContent { EntryPoint(viewModel = viewModel) }
            waitForIdle()

            enterAmount("100")

            onNodeWithTag("paceAfterThis").assertIsDisplayed()
            onNodeWithText("After this: 75.00 CHF/day left this month").assertIsDisplayed()
        }
    }

    "hides the caption for income" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seedPaceHistory(entryDao, accountDao, categoryDao)

            setContent { EntryPoint(viewModel = viewModel) }
            waitForIdle()

            enterAmount("100")
            onNodeWithTag("paceAfterThis").assertIsDisplayed()

            onNodeWithText("Income").performClick()
            waitForIdle()

            onNodeWithTag("paceAfterThis").assertDoesNotExist()
        }
    }

    "hides the caption with hidden totals" {
        withEntryPoint(clock = FakeClock(), hideEntryTotal = true) { entryDao, accountDao, categoryDao, viewModel ->
            seedPaceHistory(entryDao, accountDao, categoryDao)

            setContent { EntryPoint(viewModel = viewModel) }
            waitForIdle()

            enterAmount("100")

            onNodeWithTag("paceAfterThis").assertDoesNotExist()
        }
    }
})

private suspend fun seedPaceHistory(entryDao: EntryDao, accountDao: AccountDao, categoryDao: CategoryDao) {
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
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.enterAmount(text: String) {
    onNodeWithText("Amount").performClick()
    waitForIdle()
    text.forEach { char ->
        onNodeWithText(char.toString()).performClick()
    }
    onNodeWithText("Enter").performClick()
    waitForIdle()
}
