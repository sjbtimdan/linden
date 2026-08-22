package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.withLedgerViewModel

@OptIn(ExperimentalTestApi::class)
class LedgerViewModelTest : StringSpec({
    "new entry state prefills from the most recent entry of the same type" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

            val state = viewModel.newEntryState(EntryType.Expense)

            state.editing shouldBe null
            state.type shouldBe EntryType.Expense
            state.amountText shouldBe ""
            state.categoryId shouldBe groceries.id
            state.accountId shouldBe main.id
            state.description shouldBe "Lunch"
        }
    }

    "new entry state for a type with no entries is empty" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            val state = viewModel.newEntryState(EntryType.Income)

            state.editing shouldBe null
            state.amountText shouldBe ""
            state.categoryId shouldBe null
            state.accountId shouldBe null
            state.toAccountId shouldBe null
            state.description shouldBe ""
        }
    }

    "new transfer state prefills accounts from the most recent transfer" {
        withLedgerViewModel { entryDao, accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)
            val accounts = accountDao.getAll().first()
            val main = accounts.first()
            val savings = accounts.last()

            viewModel.createEntry(
                TransferEntry(0, null, "Move money", main, 10_000, toAccount = savings, toAmount = 9_500),
            )

            val state = viewModel.newEntryState(EntryType.Transfer)

            state.type shouldBe EntryType.Transfer
            state.amountText shouldBe ""
            state.toAmountText shouldBe ""
            state.accountId shouldBe main.id
            state.toAccountId shouldBe savings.id
            state.description shouldBe "Move money"
        }
    }

    "draft starts empty until seeded" {
        withLedgerViewModel { viewModel ->
            viewModel.draft.value.shouldBeNull()
        }
    }

    "seedDraft prefills from the latest expense" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))

            viewModel.seedDraft()

            viewModel.draft.value.let { draft ->
                draft.shouldNotBeNull()
                draft.type shouldBe EntryType.Expense
                draft.description shouldBe "Coffee"
                draft.categoryId shouldBe groceries.id
                draft.accountId shouldBe main.id
                draft.amountText shouldBe ""
            }
        }
    }

    "seedDraft does not replace an existing draft" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.seedDraft()
            viewModel.onDescriptionChange("Edited")

            viewModel.seedDraft()

            viewModel.draft.value?.description shouldBe "Edited"
        }
    }

    "selectType carries over the common fields" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.seedDraft()
            viewModel.onAmountChange("4.50")
            viewModel.onDescriptionChange("Coffee")

            viewModel.selectType(EntryType.Income)

            viewModel.selectedType.value shouldBe EntryType.Income
            viewModel.draft.value.let { draft ->
                draft.shouldNotBeNull()
                draft.type shouldBe EntryType.Income
                draft.amountText shouldBe "4.50"
                draft.description shouldBe "Coffee"
                draft.categoryId.shouldBeNull()
            }
        }
    }

    "selectType to transfer prefills accounts from the latest transfer" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            val savings = accountDao.getAll().first().first { it.name == "Savings" }
            entryDao.create(
                TransferEntry(
                    id = 0,
                    category = null,
                    description = "Move",
                    account = main,
                    amount = 500,
                    toAccount = savings,
                    toAmount = null,
                ),
            )

            viewModel.selectType(EntryType.Transfer)

            viewModel.draft.value.let { draft ->
                draft.shouldNotBeNull()
                draft.type shouldBe EntryType.Transfer
                draft.accountId shouldBe main.id
                draft.toAccountId shouldBe savings.id
                draft.description shouldBe "Move"
            }
        }
    }

    "field setters update the draft" {
        withLedgerViewModel { viewModel ->
            viewModel.seedDraft()

            viewModel.onAmountChange("7.25")
            viewModel.onDescriptionChange("Train")

            viewModel.draft.value.let { draft ->
                draft?.amountText shouldBe "7.25"
                draft?.description shouldBe "Train"
            }
        }
    }

    "clearDraft resets to an empty form" {
        withLedgerViewModel { viewModel ->
            viewModel.seedDraft()
            viewModel.onDescriptionChange("Coffee")

            viewModel.clearDraft()

            viewModel.draft.value.let { draft ->
                draft.shouldNotBeNull()
                draft.description shouldBe ""
                draft.categoryId.shouldBeNull()
                draft.accountId.shouldBeNull()
            }
        }
    }

    "saveDraft creates the entry and resets the form prefilled from it" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.seedDraft()
            viewModel.onAmountChange("4.50")
            viewModel.onCategoryChange(groceries.id)
            viewModel.onAccountChange(main.id)
            viewModel.onDescriptionChange("Coffee")

            viewModel.saveDraft() shouldBe true

            entryDao.getAll().first().filterIsInstance<ExpenseEntry>() shouldHaveSize 1
            entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first().description shouldBe "Coffee"
            viewModel.draft.value.let { draft ->
                draft.shouldNotBeNull()
                draft.amountText shouldBe ""
                draft.description shouldBe "Coffee"
                draft.categoryId shouldBe groceries.id
                draft.accountId shouldBe main.id
            }
        }
    }

    "saveDraft returns false and keeps the draft when invalid" {
        withLedgerViewModel { viewModel ->
            viewModel.seedDraft()

            viewModel.saveDraft() shouldBe false

            viewModel.draft.value.shouldNotBeNull()
        }
    }
})

private suspend fun seed(accountDao: AccountDao, categoryDao: CategoryDao): Pair<Account, Category> {
    accountDao.create("Main", Currency.CHF)
    categoryDao.create("Groceries", CategoryType.Expense)
    val main = accountDao.getAll().first().first()
    val groceries = categoryDao.getAll().first().first()
    return main to groceries
}
