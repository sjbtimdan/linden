package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.seed
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.time.TEST_NOW
import org.sjbtimdan.linden.ui.enterAmount
import org.sjbtimdan.linden.ui.withEntryPoint

@OptIn(ExperimentalTestApi::class)
class EntryPointSaveTest : StringSpec({
    "shows the expense form with add disabled initially" {
        withEntryPoint(clock = FakeClock()) { viewModel ->
            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Amount").assertIsDisplayed()
            onNodeWithText("Date & time").assertIsDisplayed()
            onNodeWithText("Add").assertIsNotEnabled()
        }
    }

    "creating an expense saves it and resets the form" {
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
            onNodeWithText("Description (optional)").performTextInput("Coffee")
            // Typing hides the type tabs; tapOutside restores them.
            onNodeWithText("Expense").assertDoesNotExist()
            tapOutside()
            onNodeWithText("Add").performClick()

            onNodeWithText("Added").assertIsDisplayed()
            // The amount is cleared for the next entry.
            onNode(hasSetTextAction() and hasText("12.50")).assertDoesNotExist()
            // The description is prefilled from the saved entry.
            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
            entryDao.getAll().first().filterIsInstance<ExpenseEntry>().shouldHaveSize(1)
        }
    }

    "adding shows the last added entry as a receipt until clear" {
        withEntryPoint(clock = FakeClock()) { _, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            enterAmount("12.50")
            onNodeWithText("Add").performClick()
            waitForIdle()

            onNodeWithTag("lastAddedEntry").assertIsDisplayed()
            onNodeWithText("Added").assertIsDisplayed()
            onNodeWithText("Groceries · Main").assertIsDisplayed()
            onNodeWithText("− 12.50 CHF").assertIsDisplayed()
            onNodeWithText("Undo").assertIsDisplayed()

            onNodeWithText("Clear").performClick()
            waitForIdle()

            onNodeWithTag("lastAddedEntry").assertDoesNotExist()
        }
    }

    "tapping the undo action on the added receipt pulls the entry back into the form" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            enterAmount("12.50")
            onNodeWithText("Description (optional)").performTextInput("Coffee")
            tapOutside()
            onNodeWithText("Add").performClick()
            waitForIdle()
            entryDao.getAll().first().shouldHaveSize(1)

            onNodeWithTag("undoAddedEntry").performClick()
            waitForIdle()

            // The entry left the ledger and its values are back in an editable form.
            onNodeWithTag("lastAddedEntry").assertDoesNotExist()
            entryDao.getAll().first().shouldBeEmpty()
            onNode(hasSetTextAction() and hasText("12.50")).assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
            onNodeWithText("Groceries").assertIsDisplayed()
            onNodeWithText("Main").assertIsDisplayed()
            onNodeWithText("Add").assertIsEnabled()
        }
    }

    "tapping edit on the added receipt loads the entry for updating" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            enterAmount("12.50")
            onNodeWithText("Description (optional)").performTextInput("Coffee")
            tapOutside()
            onNodeWithText("Add").performClick()
            waitForIdle()

            onNodeWithTag("editAddedEntry").performClick()
            waitForIdle()

            // The entry stays in the ledger and its values are back in the form,
            // now saving an update instead of a new row.
            onNodeWithTag("lastAddedEntry").assertDoesNotExist()
            entryDao.getAll().first().shouldHaveSize(1)
            onNode(hasSetTextAction() and hasText("12.50")).assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
            onNodeWithText("Save").assertIsDisplayed()

            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()
            onNodeWithText("Description (optional)").performTextInput("Tea")
            tapOutside()
            onNodeWithText("Save").performClick()
            waitForIdle()

            onNodeWithText("Updated").assertIsDisplayed()
            entryDao.getAll().first().let { entries ->
                entries shouldHaveSize 1
                entries.single().description shouldBe "Tea"
                entries.single().amount shouldBe 1_250
            }
        }
    }

    "the calculator while editing an entry shows Enter" {
        withEntryPoint(clock = FakeClock()) { _, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.loadDraft(
                EntryDraft.forEdit(
                    ExpenseEntry(1, groceries, "Coffee", main, 1_250, createdAt = TEST_NOW),
                ),
            )

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Amount").performClick()
            waitForIdle()

            onNodeWithText("Enter").assertIsDisplayed()
        }
    }

    "tapping the confirmation icon dismisses the added receipt without undoing" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            enterAmount("12.50")
            onNodeWithText("Add").performClick()
            waitForIdle()

            onNodeWithTag("dismissAddedEntry").performClick()
            waitForIdle()

            // The receipt is gone but the entry stays in the ledger.
            onNodeWithTag("lastAddedEntry").assertDoesNotExist()
            entryDao.getAll().first().shouldHaveSize(1)
        }
    }

    "the added receipt disappears after the undo window" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            enterAmount("12.50")
            onNodeWithText("Add").performClick()
            waitForIdle()

            onNodeWithTag("lastAddedEntry").assertIsDisplayed()

            mainClock.advanceTimeBy(5_000)
            waitForIdle()

            // The undo offer expired; the entry stays in the ledger.
            onNodeWithTag("lastAddedEntry").assertDoesNotExist()
            entryDao.getAll().first().shouldHaveSize(1)
        }
    }

    "clear resets the form without adding an entry" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            // The prior entry prefills the form, so Clear must empty it, not restore the prefill.
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = TEST_NOW))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            waitForText("Coffee")
            enterAmount("12.50")
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()
            onNodeWithText("Description (optional)").performTextInput("Lunch")
            tapOutside()
            onNodeWithText("Add").assertIsEnabled()

            onNodeWithText("Clear").performClick()

            onNode(hasSetTextAction() and hasText("12.50")).assertDoesNotExist()
            onNode(hasSetTextAction() and hasText("Lunch")).assertDoesNotExist()
            // Not restored to the last entry's prefill either.
            onNode(hasSetTextAction() and hasText("Coffee")).assertDoesNotExist()
            onNodeWithText("Groceries").assertDoesNotExist()
            onNodeWithText("Main").assertDoesNotExist()
            // The emptied form is not flagged as missing an amount.
            onNodeWithText("Enter an amount").assertDoesNotExist()
            entryDao.getAll().first().filterIsInstance<ExpenseEntry>().shouldHaveSize(1)
        }
    }

    "back arrow while editing expands the form and keeps the draft" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            enterAmount("12.50")
            onNodeWithText("Description (optional)").performTextInput("Lunch")

            // The action buttons hide while a field is focused.
            onNodeWithTag("saveEntry").assertDoesNotExist()

            // Invoke the click action directly: the skiko mouse-click path misses the
            // collapsed button, so a gesture tap would test injection, not the back logic.
            onNodeWithContentDescription("Back")
                .performSemanticsAction(SemanticsActions.OnClick)

            onNode(hasSetTextAction() and hasText("12.50")).assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("Lunch")).assertIsDisplayed()
            onNodeWithTag("saveEntry").assertIsDisplayed()
            entryDao.getAll().first().shouldHaveSize(0)
        }
    }

    "system back keeps a half-finished entry instead of clearing it" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            var systemBack: (() -> Unit)? = null
            setContent {
                EntryPoint(
                    viewModel = viewModel,
                    systemBackHandler = { enabled, onBack -> if (enabled) systemBack = onBack },
                )
            }

            enterAmount("12.50")
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            waitForIdle()

            // Both pickers committed and dropped focus, as in the reported repro.
            systemBack shouldNotBe null
            systemBack!!.invoke()
            waitForIdle()

            // Back exits editing but never wipes the draft.
            onNode(hasSetTextAction() and hasText("12.50")).assertIsDisplayed()
            onNodeWithText("Groceries").assertIsDisplayed()
            onNodeWithText("Main").assertIsDisplayed()
            onNodeWithTag("saveEntry").assertIsEnabled()
            entryDao.getAll().first().shouldHaveSize(0)
        }
    }
})
