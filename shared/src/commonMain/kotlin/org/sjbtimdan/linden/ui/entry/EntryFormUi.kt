package org.sjbtimdan.linden.ui.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.EntryType

/** Values the entry form derives from its inputs, shared by its field sections. */
@Immutable
internal data class EntryFormUi(
    val accounts: List<Account>,
    val visibleCategories: List<Category>,
    val fromAccount: Account?,
    val toAccount: Account?,
    val toAccountOptions: List<Account>,
    val selectedCategory: Category?,
    val predictedCategoryOptions: List<Category>,
    val predictedAccountOptions: List<Account>,
    val showReceivedAmount: Boolean,
)

internal fun entryFormUi(
    state: EntryDraft,
    accounts: List<Account>,
    categories: List<Category>,
    suggestions: EntryFormSuggestions,
): EntryFormUi {
    val visibleCategories = categoriesForType(categories, state.type)
    val fromAccount = accounts.firstOrNull { it.id == state.accountId }
    val toAccount = accounts.firstOrNull { it.id == state.toAccountId }
    return EntryFormUi(
        accounts = accounts,
        visibleCategories = visibleCategories,
        fromAccount = fromAccount,
        toAccount = toAccount,
        toAccountOptions = accounts.filter { it.id != state.accountId },
        selectedCategory = visibleCategories.firstOrNull { it.id == state.categoryId },
        predictedCategoryOptions = suggestions.categoryIds
            .filterNot { it == state.categoryId }
            .mapNotNull { id -> visibleCategories.firstOrNull { it.id == id } },
        predictedAccountOptions = suggestions.accountIds
            .filterNot { it == state.accountId }
            .mapNotNull { id -> accounts.firstOrNull { it.id == id } },
        showReceivedAmount = state.type == EntryType.Transfer &&
            fromAccount != null && toAccount != null &&
            fromAccount.currency != toAccount.currency,
    )
}

/** Derives [EntryFormUi] only when a field it reads changes, not on every keystroke. */
@Composable
internal fun rememberEntryFormUi(
    state: EntryDraft,
    accounts: List<Account>,
    categories: List<Category>,
    suggestions: EntryFormSuggestions,
): EntryFormUi = remember(
    state.type,
    state.accountId,
    state.toAccountId,
    state.categoryId,
    accounts,
    categories,
    suggestions.categoryIds,
    suggestions.accountIds,
) {
    entryFormUi(state, accounts, categories, suggestions)
}
