package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.withEntryPoint
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class EntryPointViewModelTest : StringSpec({
    "new entry state prefills from the most recent entry of the same type" {
        withEntryPoint { entryDao, accountDao, categoryDao, viewModel ->
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
        withEntryPoint { entryDao, accountDao, categoryDao, viewModel ->
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
        withEntryPoint { entryDao, accountDao, _, viewModel ->
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
        withEntryPoint { viewModel ->
            viewModel.draft.value.shouldBeNull()
        }
    }

    "seedDraft prefills from the latest expense" {
        withEntryPoint { entryDao, accountDao, categoryDao, viewModel ->
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
        withEntryPoint { accountDao, categoryDao, viewModel ->
            val (_, _) = seed(accountDao, categoryDao)
            viewModel.seedDraft()
            viewModel.onDescriptionChange("Edited")

            viewModel.seedDraft()

            viewModel.draft.value?.description shouldBe "Edited"
        }
    }

    "selectType carries over the common fields" {
        withEntryPoint { accountDao, categoryDao, viewModel ->
            val (_, _) = seed(accountDao, categoryDao)
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
        withEntryPoint { entryDao, accountDao, categoryDao, viewModel ->
            val (main, _) = seed(accountDao, categoryDao)
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
        withEntryPoint { viewModel ->
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
        withEntryPoint { viewModel ->
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
        withEntryPoint { entryDao, accountDao, categoryDao, viewModel ->
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
        withEntryPoint { viewModel ->
            viewModel.seedDraft()

            viewModel.saveDraft() shouldBe false

            viewModel.draft.value.shouldNotBeNull()
        }
    }

    "account and category suggestions are empty without a draft" {
        withEntryPoint { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))

            viewModel.accountSuggestions.first().shouldBeEmpty()
            viewModel.categorySuggestions.first().shouldBeEmpty()
        }
    }

    "account and category suggestions reflect the draft and history" {
        withEntryPoint { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            categoryDao.create("Leisure", CategoryType.Expense)
            val savings = accountDao.getAll().first().first { it.name == "Savings" }
            val leisure = categoryDao.getAll().first().first { it.name == "Leisure" }
            val now = Clock.System.now()
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))
            entryDao.create(ExpenseEntry(0, leisure, "Cinema", savings, 2_000, createdAt = now.minus(1.days)))

            viewModel.clearDraft()
            viewModel.onAmountChange("4.50")

            viewModel.categorySuggestions.first { it.isNotEmpty() } shouldContainExactly listOf(groceries.id)
            viewModel.accountSuggestions.first { it.isNotEmpty() } shouldContainExactly listOf(main.id)
        }
    }

    "total balance is the initial balance plus income minus expenses" {
        withEntryPoint { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            categoryDao.create("Salary", CategoryType.Income)
            val main = accountDao.getAll().first().first { it.name == "Main" }
            val groceries = categoryDao.getAll().first().first { it.name == "Groceries" }
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Rent", main, 5_000))
            viewModel.createEntry(IncomeEntry(0, salary, "Salary", main, 2_000))

            viewModel.totalMinor.first() shouldBe 10_000 - 450 - 5_000 + 2_000
        }
    }

    "transfers move money between accounts without changing the total" {
        withEntryPoint { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            accountDao.create("Savings", Currency.CHF)
            val main = accountDao.getAll().first().first { it.name == "Main" }
            val savings = accountDao.getAll().first().first { it.name == "Savings" }

            viewModel.createEntry(TransferEntry(0, null, "Move", main, 3_000, toAccount = savings, toAmount = null))

            viewModel.totalMinor.first() shouldBe 10_000
        }
    }

    "foreign balances are converted with the stored rates" {
        withEntryPoint(
            rates = listOf(FxRate(Currency.CHF, Currency.EUR, 0.9, "2026-01-01")),
        ) { accountDao, _, viewModel ->
            accountDao.create("Wallet", Currency.EUR, initialBalance = 10_000)

            // 100.00 EUR / 0.9 CHF-per-EUR = 111.11 CHF.
            viewModel.totalMinor.first() shouldBe 11_111
        }
    }

    "total is null while a foreign currency has no stored rate" {
        withEntryPoint { accountDao, _, viewModel ->
            accountDao.create("Wallet", Currency.EUR, initialBalance = 10_000)

            viewModel.totalMinor.first().shouldBeNull()
        }
    }

    "entries dated after today do not count toward the total" {
        withEntryPoint(today = { LocalDate(2026, 1, 15) }) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()
            val future = Instant.fromEpochMilliseconds(1_768_867_200_000) // 2026-01-20 00:00 UTC

            entryDao.create(
                ExpenseEntry(0, groceries, "Future", main, 450, createdAt = future, createdZone = TimeZone.UTC),
            )

            viewModel.totalMinor.first() shouldBe 10_000
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
