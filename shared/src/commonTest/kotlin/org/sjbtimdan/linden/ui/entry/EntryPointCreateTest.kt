package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.seed
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.withEntryPoint

@OptIn(ExperimentalTestApi::class)
class EntryPointCreateTest : StringSpec({
    "empty dropdowns show the create chips when no accounts or categories exist" {
        withEntryPoint(clock = FakeClock()) { viewModel ->
            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Category").performClick()
            onNodeWithText("New category").assertIsDisplayed()
            onNodeWithText("Groceries").assertDoesNotExist()
            // The empty dropdown and its chip already explain the blocker, so no generic hint.
            onNodeWithText("Enter an amount").assertDoesNotExist()
            tapOutside()

            onNodeWithText("Account").performClick()
            onNodeWithText("New account").assertIsDisplayed()
            onNodeWithText("Main").assertDoesNotExist()
        }
    }

    "transfer fields show the create chip when no accounts exist" {
        withEntryPoint(clock = FakeClock()) { viewModel ->
            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Transfer").performClick()

            onNodeWithText("From account").performClick()
            onNodeWithText("New account").assertIsDisplayed()
            tapOutside()

            onNodeWithText("To account").performClick()
            onNodeWithText("New account").assertIsDisplayed()
            onNodeWithText("New category").assertDoesNotExist()
        }
    }

    "creating a category from the chip selects it in the form" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Category").performClick()
            onNodeWithText("New category").performClick()
            waitForIdle()

            // The dialog's name field is the last text field on screen.
            onAllNodes(hasSetTextAction())[1].performTextInput("Travel")
            onNodeWithText("Save").performClick()
            waitForIdle()

            onNodeWithText("Travel").assertIsDisplayed()
            val created = categoryDao.getAll().first { list -> list.any { it.name == "Travel" } }
                .first { it.name == "Travel" }
            viewModel.draft.value?.categoryId shouldBe created.id
            entryDao.getAll().first().shouldHaveSize(0)
        }
    }

    "creating a category from the chip with an empty name shows a required-name error" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Category").performClick()
            onNodeWithText("New category").performClick()
            waitForIdle()

            onNodeWithText("Save").performClick()
            waitForIdle()

            onNodeWithText("A name is required").assertIsDisplayed()
            categoryDao.getAll().first().shouldHaveSize(1)
            entryDao.getAll().first().shouldBeEmpty()
        }
    }

    "creating an account from the chip selects it in the form" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Account").performClick()
            onNodeWithText("New account").performClick()
            waitForIdle()

            onAllNodes(hasSetTextAction())[1].performTextInput("Wallet")
            onNodeWithText("Save").performClick()
            waitForIdle()

            onNodeWithText("Wallet").assertIsDisplayed()
            val created = accountDao.getAll().first { list -> list.any { it.name == "Wallet" } }
                .first { it.name == "Wallet" }
            viewModel.draft.value?.accountId shouldBe created.id
        }
    }

    "creating an account from the chip with an empty name shows a required-name error" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Account").performClick()
            onNodeWithText("New account").performClick()
            waitForIdle()

            onNodeWithText("Save").performClick()
            waitForIdle()

            onNodeWithText("A name is required").assertIsDisplayed()
            accountDao.getAll().first().shouldHaveSize(1)
            entryDao.getAll().first().shouldBeEmpty()
        }
    }

    "creating a transfer destination from the chip selects it as the to account" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Transfer").performClick()
            onNodeWithText("To account").performClick()
            onNodeWithText("New account").performClick()
            waitForIdle()

            onAllNodes(hasSetTextAction())[1].performTextInput("Savings")
            onNodeWithText("Save").performClick()
            waitForIdle()

            onNodeWithText("Savings").assertIsDisplayed()
            val created = accountDao.getAll().first { list -> list.any { it.name == "Savings" } }
                .first { it.name == "Savings" }
            viewModel.draft.value?.toAccountId shouldBe created.id
        }
    }

    "to account dropdown offers the create chip when only one account exists" {
        withEntryPoint(clock = FakeClock()) { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Transfer").performClick()
            onNodeWithText("To account").performClick()

            // The from account is excluded, so the To list is empty but offers creation.
            onNodeWithText("New account").assertIsDisplayed()
            onNodeWithText("Please enter account").assertDoesNotExist()

            onNodeWithText("New account").performClick()
            onNodeWithText("Save").assertIsDisplayed()
            onNodeWithText("Cancel").performClick()
        }
    }

    "to account dropdown excludes the from account" {
        withEntryPoint(clock = FakeClock()) { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Transfer").performClick()

            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()

            onNodeWithText("To account").performClick()
            // Collapsed to To account, so Main's absence proves it is not offered as a target.
            onNodeWithText("Main").assertDoesNotExist()
            onNodeWithText("Savings").assertIsDisplayed()
        }
    }
})
