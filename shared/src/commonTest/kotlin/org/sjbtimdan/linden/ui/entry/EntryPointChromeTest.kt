package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.seed
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.time.TEST_NOW
import org.sjbtimdan.linden.ui.enterAmount
import org.sjbtimdan.linden.ui.rates.RatesWarning
import org.sjbtimdan.linden.ui.withEntryPoint

@OptIn(ExperimentalTestApi::class)
class EntryPointChromeTest : StringSpec({
    "hero card shows the total balance across accounts" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 12_345)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = TEST_NOW))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Total balance").assertIsDisplayed()
            onNodeWithText("118.95").assertIsDisplayed()
        }
    }

    "hero card masks the total when hiding is enabled" {
        withEntryPoint(clock = FakeClock(), hideEntryTotal = true) { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 12_345)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = TEST_NOW))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Total balance").assertIsDisplayed()
            onNodeWithText("••••••").assertIsDisplayed()
            onNodeWithText("118.95").assertDoesNotExist()
            onNodeWithContentDescription("Show total").assertIsDisplayed()
        }
    }

    "tapping the eye hides and restores the hero card total" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 12_345)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = TEST_NOW))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("118.95").assertIsDisplayed()
            onNodeWithContentDescription("Hide total").performClick()
            waitForIdle()

            onNodeWithText("••••••").assertIsDisplayed()
            onNodeWithText("118.95").assertDoesNotExist()
            viewModel.hideTotal.value shouldBe true

            onNodeWithContentDescription("Show total").performClick()
            waitForIdle()

            onNodeWithText("118.95").assertIsDisplayed()
            onNodeWithText("••••••").assertDoesNotExist()
            viewModel.hideTotal.value shouldBe false
        }
    }

    "hero card opens insights" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 12_345)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = TEST_NOW))

            var navigatedToInsights = false
            setContent {
                EntryPoint(
                    viewModel = viewModel,
                    onNavigateToInsights = { navigatedToInsights = true },
                )
            }

            onNodeWithTag("entryInsightsAction").performClick()

            navigatedToInsights shouldBe true
        }
    }

    "expense is the default selected type" {
        withEntryPoint(clock = FakeClock()) { viewModel ->
            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Expense").assertIsSelected()
            onNodeWithText("Income").assertIsNotSelected()
            onNodeWithText("Transfer").assertIsNotSelected()
        }
    }

    "the hero card compacts once the draft is touched and expands again on clear" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = TEST_NOW))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithTag("totalBalanceCard").assertIsDisplayed()
            onNodeWithTag("totalBalanceCompact").assertDoesNotExist()

            enterAmount("12.50")

            onNodeWithTag("totalBalanceCompact").assertIsDisplayed()
            onNodeWithTag("totalBalanceCard").assertDoesNotExist()
            onNodeWithText("Total balance").assertIsDisplayed()

            onNodeWithText("Clear").performClick()
            waitForIdle()

            onNodeWithTag("totalBalanceCard").assertIsDisplayed()
            onNodeWithTag("totalBalanceCompact").assertDoesNotExist()
        }
    }

    "chrome stays compact after adding an entry" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            enterAmount("12.50")
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("Add").performClick()
            waitForIdle()

            onNodeWithTag("totalBalanceCompact").assertIsDisplayed()
            onNodeWithTag("totalBalanceCard").assertDoesNotExist()
            onNodeWithText("Added").assertIsDisplayed()
        }
    }

    "the rates banner compacts while a draft is in progress" {
        withEntryPoint(clock = FakeClock(), ratesSeen = true) { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel, ratesWarning = RatesWarning.Missing)
            }

            onNodeWithTag("ratesWarningBanner").assertIsDisplayed()
            onNodeWithTag("ratesWarningBannerCompact").assertDoesNotExist()

            enterAmount("12.50")

            onNodeWithTag("ratesWarningBannerCompact").assertIsDisplayed()
            onNodeWithTag("ratesWarningBanner").assertDoesNotExist()
            onNodeWithText("Set rates").assertIsDisplayed()
        }
    }

    "shows a banner when rates are missing" {
        withEntryPoint(clock = FakeClock(), ratesSeen = true) { viewModel ->
            setContent {
                EntryPoint(viewModel = viewModel, ratesWarning = RatesWarning.Missing)
            }

            onNodeWithText("No exchange rates available. You can set them manually.").assertIsDisplayed()
        }
    }

    "shows a banner when rates are over a week old" {
        withEntryPoint(clock = FakeClock(), ratesSeen = true) { viewModel ->
            setContent {
                EntryPoint(viewModel = viewModel, ratesWarning = RatesWarning.Outdated)
            }

            onNodeWithText("Exchange rates are over a week old. You can set them manually.").assertIsDisplayed()
        }
    }

    "the rates banner navigates to the rates screen" {
        withEntryPoint(clock = FakeClock(), ratesSeen = true) { viewModel ->
            var navigatedToRates = false
            setContent {
                EntryPoint(
                    viewModel = viewModel,
                    ratesWarning = RatesWarning.Missing,
                    onNavigateToRates = { navigatedToRates = true },
                )
            }

            onNodeWithText("Set rates").performClick()
            navigatedToRates shouldBe true
        }
    }

    "hero card is hidden until the first entry exists" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithTag("totalBalanceCard").assertDoesNotExist()
            onNodeWithText("Total balance").assertDoesNotExist()

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = TEST_NOW))
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Total balance").fetchSemanticsNodes().isNotEmpty()
            }

            onNodeWithTag("totalBalanceCard").assertIsDisplayed()
        }
    }

    "rates banner is hidden on a cold start" {
        withEntryPoint(clock = FakeClock()) { viewModel ->
            setContent {
                EntryPoint(viewModel = viewModel, ratesWarning = RatesWarning.Missing)
            }

            onNodeWithTag("ratesWarningBanner").assertDoesNotExist()
            onNodeWithText("No exchange rates available. You can set them manually.").assertDoesNotExist()
        }
    }

    "rates banner shows once entries span two currencies" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)
            categoryDao.create("Groceries", CategoryType.Expense)
            val accounts = accountDao.getAll().first()
            val groceries = categoryDao.getAll().first().first()
            val main = accounts.first { it.name == "Main" }
            val savings = accounts.first { it.name == "Savings" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = TEST_NOW))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", savings, 900, createdAt = TEST_NOW))

            setContent {
                EntryPoint(viewModel = viewModel, ratesWarning = RatesWarning.Missing)
            }

            waitForText("No exchange rates available. You can set them manually.")
            onNodeWithText("No exchange rates available. You can set them manually.").assertIsDisplayed()
        }
    }

    "rates banner shows once the rates screen has been opened" {
        withEntryPoint(clock = FakeClock(), ratesSeen = true) { viewModel ->
            setContent {
                EntryPoint(viewModel = viewModel, ratesWarning = RatesWarning.Missing)
            }

            onNodeWithText("No exchange rates available. You can set them manually.").assertIsDisplayed()
        }
    }
})
