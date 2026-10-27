package org.sjbtimdan.linden

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.capture.PendingCapture
import org.sjbtimdan.linden.data.FakeFxRatesSource
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.time.TEST_NOW
import org.sjbtimdan.linden.ui.ledger.LedgerPeriod
import org.sjbtimdan.linden.ui.withApp

@OptIn(ExperimentalTestApi::class)
class AppTest : StringSpec({
    "starts on the entry screen" {
        withApp { dependencies ->
            setContent { App(dependencies) }
            onNodeWithText("Add").assertExists()
        }
    }

    "a captured payment alert shows on the entry screen, not the ledger" {
        withApp { dependencies ->
            dependencies.captureRepository.addPending(
                PendingCapture(
                    packageName = "com.example.bank",
                    amountMinor = 2450,
                    currency = Currency.CHF,
                    description = "COOP",
                    type = EntryType.Expense,
                    capturedAtMillis = 1L,
                ),
            )
            setContent { App(dependencies) }

            onNodeWithTag("capturedPaymentsBar").assertExists()
            onNodeWithText("1 payment to review").performClick()
            onNodeWithTag("capturedPaymentsCard").assertExists()
            onNodeWithText("COOP").assertExists()

            onNodeWithText("Ledger").performClick()
            waitForIdle()

            onAllNodesWithTag("capturedPaymentsBar").assertCountEquals(0)
            onAllNodesWithTag("capturedPaymentsCard").assertCountEquals(0)

            onNodeWithText("Entry").performClick()
            onNodeWithTag("capturedPaymentsBar").assertExists()
            onNodeWithText("1 payment to review").performClick()
            onNodeWithTag("capturedPaymentsCard").assertExists()
        }
    }

    "a saved captured payment disappears from the entry alert" {
        withApp { dependencies ->
            dependencies.accountDao.create("Main", Currency.CHF)
            dependencies.categoryDao.create("Groceries", CategoryType.Expense)
            val main = dependencies.accountDao.getAll().first().first()
            val groceries = dependencies.categoryDao.getAll().first().first()

            // Teach a template for this wording so the alert's Add prefills a
            // complete draft, as a repeat notification would.
            val learned = dependencies.captureRepository.addPending(
                PendingCapture(
                    packageName = "com.example.bank",
                    amountMinor = 2450,
                    currency = Currency.CHF,
                    description = "COOP",
                    type = EntryType.Expense,
                    capturedAtMillis = 1L,
                ),
            )
            dependencies.captureRepository.completeCapture(
                learned.id,
                ExpenseEntry(0, groceries, "COOP", main, 2450, createdAt = TEST_NOW),
            )
            dependencies.captureRepository.templatesFlow().first { it.isNotEmpty() }
            val pending = dependencies.captureRepository.addPending(
                PendingCapture(
                    packageName = "com.example.bank",
                    amountMinor = 2450,
                    currency = Currency.CHF,
                    description = "COOP",
                    type = EntryType.Expense,
                    capturedAtMillis = 2L,
                ),
            )

            setContent { App(dependencies) }

            onNodeWithText("1 payment to review").performClick()
            onNodeWithTag("capturedPaymentsCard").assertExists()
            onNodeWithTag("capturedPaymentAdd-${pending.id}").performClick()
            onNodeWithTag("saveEntry").performClick()
            waitForIdle()

            dependencies.captureRepository.pending().shouldBeEmpty()
            onNodeWithTag("capturedPaymentsBar").assertDoesNotExist()
            onNodeWithTag("capturedPaymentsCard").assertDoesNotExist()
        }
    }

    "bottom navigation switches screens" {
        withApp { dependencies ->
            setContent { App(dependencies) }

            onNodeWithText("Ledger").performClick()
            // A brand-new dataset is guided to its first entry.
            onNodeWithText("No entries yet.").assertExists()
            onNodeWithText("Add your first entry").assertExists()

            onNodeWithText("Settings").performClick()
            onNodeWithText("Import from Ivy").assertExists()

            onNodeWithText("Entry").performClick()
            onNodeWithText("Add").assertExists()
        }
    }

    "the ledger keeps its entry dialog while it is the screen in front" {
        withApp { dependencies ->
            dependencies.accountDao.create("Main", Currency.CHF)
            dependencies.categoryDao.create("Groceries", CategoryType.Expense)
            val main = dependencies.accountDao.getAll().first().first()
            val groceries = dependencies.categoryDao.getAll().first().first()
            dependencies.entryDao.create(ExpenseEntry(0, groceries, "COOP", main, 2450, createdAt = TEST_NOW))
            // The fixture is dated in the past: show every period so the row is
            // on screen whatever today is.
            dependencies.ledgerViewModel.setPeriod(LedgerPeriod.All)

            setContent { App(dependencies) }

            onNodeWithText("Ledger").performClick()
            waitForIdle()
            onNodeWithText("COOP").performClick()

            onNodeWithTag("dialogTypePicker").assertExists()
            dependencies.ledgerViewModel.dialogState.value.shouldNotBeNull()
        }
    }

    "a ledger entry dialog left open does not show once the ledger is not the screen" {
        withApp { dependencies ->
            dependencies.accountDao.create("Main", Currency.CHF)
            dependencies.categoryDao.create("Groceries", CategoryType.Expense)
            val main = dependencies.accountDao.getAll().first().first()
            val groceries = dependencies.categoryDao.getAll().first().first()
            // The dialog's draft lives in the app-lifetime ledger ViewModel, so it
            // can outlive the screen that showed it; the app starts on Entry.
            dependencies.ledgerViewModel.openEditDialog(
                ExpenseEntry(0, groceries, "COOP", main, 2450, createdAt = TEST_NOW),
            )

            setContent { App(dependencies) }
            waitForIdle()

            dependencies.ledgerViewModel.dialogState.value.shouldBeNull()
            onAllNodesWithTag("dialogTypePicker").assertCountEquals(0)
        }
    }

    "settings navigates to categories and back" {
        withApp { dependencies ->
            setContent { App(dependencies) }

            onNodeWithText("Settings").performClick()
            onNodeWithText("Categories").performClick()
            onNodeWithText("No categories yet.").assertExists()

            onNodeWithContentDescription("Back").performClick()
            onNodeWithText("Import from Ivy").assertExists()
        }
    }

    "settings navigates to accounts and back" {
        withApp { dependencies ->
            setContent { App(dependencies) }

            onNodeWithText("Settings").performClick()
            onNodeWithText("Accounts").performClick()
            onNodeWithText("New Account").assertExists()

            onNodeWithContentDescription("Back").performClick()
            onNodeWithText("Import from Ivy").assertExists()
        }
    }

    "settings navigates to currency rates and back" {
        withApp { dependencies ->
            setContent { App(dependencies) }

            onNodeWithText("Settings").performClick()
            onNodeWithText("Currency rates").performClick()
            onNodeWithText("Refresh").assertExists()

            onNodeWithContentDescription("Back").performClick()
            onNodeWithText("Import from Ivy").assertExists()
        }
    }

    "startup shows a rates warning and navigates to rates when the fetch fails with no cached rates" {
        withApp(fxRatesSource = FakeFxRatesSource { error("network") }, ratesSeen = true) { dependencies ->
            setContent { App(dependencies) }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("No exchange rates available. You can set them manually.")
                    .fetchSemanticsNodes().isNotEmpty()
            }

            onNodeWithText("Set rates").performClick()
            onNodeWithText("Refresh").assertExists()
        }
    }

    "system back from a sub-screen returns to the screen it was opened from" {
        withApp(fxRatesSource = FakeFxRatesSource { error("network") }, ratesSeen = true) { dependencies ->
            var systemBack: (() -> Unit)? = null
            setContent {
                App(
                    dependencies,
                    systemBackHandler = { enabled, onBack -> if (enabled) systemBack = onBack },
                )
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("No exchange rates available. You can set them manually.")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Set rates").performClick()
            onNodeWithText("Refresh").assertExists()
            waitForIdle()

            systemBack shouldNotBe null
            systemBack!!.invoke()
            waitForIdle()

            // Rates were opened from Entry, so back returns there instead of
            // exiting the app or landing on Settings.
            onNodeWithText("Add").assertExists()
        }
    }

    "the back arrow from a sub-screen returns to the screen it was opened from" {
        withApp(fxRatesSource = FakeFxRatesSource { error("network") }, ratesSeen = true) { dependencies ->
            setContent { App(dependencies) }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("No exchange rates available. You can set them manually.")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Set rates").performClick()
            onNodeWithText("Refresh").assertExists()

            onNodeWithContentDescription("Back").performClick()

            onNodeWithText("Add").assertExists()
        }
    }

    "the active tab is marked selected in the bottom navigation" {
        withApp { dependencies ->
            setContent { App(dependencies) }

            onNodeWithText("Entry").assertIsSelected()
            onNodeWithText("Ledger").assertIsNotSelected()

            onNodeWithText("Ledger").performClick()
            onNodeWithText("Ledger").assertIsSelected()
            onNodeWithText("Entry").assertIsNotSelected()
        }
    }

    "swiping moves between the top-level screens and follows the bottom bar" {
        withApp { dependencies ->
            setContent { App(dependencies) }

            // Entry → Settings.
            swipeTopLevelPager(forward = true)
            waitForIdle()
            onNodeWithText("Import from Ivy").assertExists()
            onNode(hasText("Settings") and isSelectable()).assertIsSelected()

            // Settings → Entry.
            swipeTopLevelPager(forward = false)
            waitForIdle()
            onNodeWithText("Add").assertExists()
            onNode(hasText("Entry") and isSelectable()).assertIsSelected()

            // Entry → Ledger.
            swipeTopLevelPager(forward = false)
            waitForIdle()
            onNodeWithText("No entries yet.").assertExists()
            onNode(hasText("Ledger") and isSelectable()).assertIsSelected()
        }
    }
})

/**
 * Swipes the top-level pager one page forward (toward Settings) or back,
 * near its top edge so the gesture starts clear of form text fields.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.swipeTopLevelPager(forward: Boolean) {
    onNodeWithTag("topLevelPager").performTouchInput {
        val y = top + 40f
        val fromX = if (forward) right - 16f else left + 16f
        val toX = if (forward) left + 16f else right - 16f
        swipe(start = Offset(fromX, y), end = Offset(toX, y), durationMillis = 200)
    }
}
