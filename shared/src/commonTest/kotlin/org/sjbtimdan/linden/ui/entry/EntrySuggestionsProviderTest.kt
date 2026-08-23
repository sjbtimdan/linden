package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.ui.onTestMain
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class EntrySuggestionsProviderTest : StringSpec({
    "suggestions are empty without a draft" {
        withSuggestionsProvider { entryDao, accountDao, categoryDao, provider, _ ->
            val (main, groceries) = seed(accountDao, categoryDao)
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))

            provider.accountSuggestions.first().shouldBeEmpty()
            provider.categorySuggestions.first().shouldBeEmpty()
            provider.descriptionSuggestions.first().shouldBeEmpty()
        }
    }

    "suggestions are empty while editing an existing entry" {
        withSuggestionsProvider { entryDao, accountDao, categoryDao, provider, draft ->
            val (main, groceries) = seed(accountDao, categoryDao)
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))
            draft.value = EntryDraft.forEdit(ExpenseEntry(1, groceries, "Coffee", main, 450))

            provider.accountSuggestions.first().shouldBeEmpty()
            provider.categorySuggestions.first().shouldBeEmpty()
            provider.descriptionSuggestions.first().shouldBeEmpty()
        }
    }

    "suggestions reflect the draft and recent history" {
        withSuggestionsProvider { entryDao, accountDao, categoryDao, provider, draft ->
            val (main, groceries) = seed(accountDao, categoryDao)
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = Clock.System.now()))
            draft.value = EntryDraft.forNew(EntryType.Expense)
                .copy(amountText = "4.50", categoryId = groceries.id, accountId = main.id)

            provider.accountSuggestions.awaitNotEmpty() shouldContainExactly listOf(main.id)
            provider.categorySuggestions.awaitNotEmpty() shouldContainExactly listOf(groceries.id)
            provider.descriptionSuggestions.awaitNotEmpty() shouldContainExactly listOf("Coffee")
        }
    }

    "entries older than the prediction horizon are not considered" {
        withSuggestionsProvider { entryDao, accountDao, categoryDao, provider, draft ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("Old", Currency.CHF)
            categoryDao.create("Ancient", CategoryType.Expense)
            val oldAccount = accountDao.getAll().first().first { it.name == "Old" }
            val ancientCategory = categoryDao.getAll().first().first { it.name == "Ancient" }
            val monthsAgo = Clock.System.now().minus(7, DateTimeUnit.MONTH, TimeZone.currentSystemDefault())
            entryDao.create(ExpenseEntry(0, ancientCategory, "Ancient", oldAccount, 450, createdAt = monthsAgo))
            entryDao.create(ExpenseEntry(0, groceries, "Lunch", main, 450, createdAt = Clock.System.now()))
            draft.value = EntryDraft.forNew(EntryType.Expense)
                .copy(amountText = "4.50", categoryId = groceries.id, accountId = main.id)

            provider.accountSuggestions.awaitNotEmpty() shouldContainExactly listOf(main.id)
            provider.categorySuggestions.awaitNotEmpty() shouldContainExactly listOf(groceries.id)
            provider.descriptionSuggestions.awaitNotEmpty() shouldContainExactly listOf("Lunch")
        }
    }

    "suggestions update when the draft changes" {
        withSuggestionsProvider { entryDao, accountDao, categoryDao, provider, draft ->
            val (main, groceries) = seed(accountDao, categoryDao)
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = Clock.System.now()))
            draft.value = EntryDraft.forNew(EntryType.Expense)

            provider.accountSuggestions.first().shouldBeEmpty()
            provider.categorySuggestions.first().shouldBeEmpty()

            draft.value = draft.value?.copy(amountText = "4.50")

            provider.accountSuggestions.awaitNotEmpty() shouldContainExactly listOf(main.id)
            provider.categorySuggestions.awaitNotEmpty() shouldContainExactly listOf(groceries.id)
        }
    }
})

@OptIn(ExperimentalTestApi::class)
private fun withSuggestionsProvider(
    block: suspend ComposeUiTest.(
        entryDao: EntryDao,
        accountDao: AccountDao,
        categoryDao: CategoryDao,
        provider: EntrySuggestionsProvider,
        draft: MutableStateFlow<EntryDraft?>,
    ) -> Unit,
) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val entryDao = EntryDao(database.entryQueries)
            val accountDao = AccountDao(database.accountQueries)
            val categoryDao = CategoryDao(database.categoryQueries)
            val draft = MutableStateFlow<EntryDraft?>(null)
            val provider = EntrySuggestionsProvider(entryDao, draft, CoroutineScope(Dispatchers.Main))
            block(entryDao, accountDao, categoryDao, provider, draft)
        }
    }
}

/**
 * Awaits the first emission produced once the entry window has been loaded,
 * since the state flow's initial value is emitted before the SQLDelight query
 * completes.
 */
private suspend fun <T> StateFlow<List<T>>.awaitNotEmpty(): List<T> =
    withTimeout(5.seconds) { first { it.isNotEmpty() } }

private suspend fun seed(accountDao: AccountDao, categoryDao: CategoryDao): Pair<Account, Category> {
    accountDao.create("Main", Currency.CHF)
    categoryDao.create("Groceries", CategoryType.Expense)
    val main = accountDao.getAll().first().first()
    val groceries = categoryDao.getAll().first().first()
    return main to groceries
}
