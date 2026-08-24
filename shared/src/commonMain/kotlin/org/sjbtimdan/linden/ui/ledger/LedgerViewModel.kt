package org.sjbtimdan.linden.ui.ledger

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.entry.EntryDraft
import org.sjbtimdan.linden.ui.entry.EntryEditorViewModel
import org.sjbtimdan.linden.ui.entry.EntrySuggestionsProvider
import org.sjbtimdan.linden.ui.entry.formatAmount

class LedgerViewModel(
    entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
) : EntryEditorViewModel(entryDao, accountDao, categoryDao) {
    private val suggestions = EntrySuggestionsProvider(entryDao, draft, viewModelScope)

    /** Most likely account ids for the current draft; only for new entries. */
    val accountSuggestions: StateFlow<List<Long>> get() = suggestions.accountSuggestions

    /** Most likely category ids for the current draft; only for new entries. */
    val categorySuggestions: StateFlow<List<Long>> get() = suggestions.categorySuggestions

    /** Most likely descriptions for the current draft; only for new entries. */
    val descriptionSuggestions: StateFlow<List<String>> get() = suggestions.descriptionSuggestions

    /** Whole entries the user is likely to repeat right now, ranked time first. */
    val quickEntries: StateFlow<List<Entry>> get() = suggestions.quickEntries

    /** Fills the draft from a quick-entry chip, keeping the current date and time. */
    fun applyQuickEntry(entry: Entry) = draftState.update { state ->
        if (state == null) {
            null
        } else {
            when (entry) {
                is TransferEntry -> state.copy(
                    type = EntryType.Transfer,
                    amountText = formatAmount(entry.amount),
                    categoryId = entry.category?.id,
                    accountId = entry.account.id,
                    toAccountId = entry.toAccount.id,
                    toAmountText = formatAmount(entry.toAmount ?: entry.amount),
                    description = entry.description.orEmpty(),
                )

                is ExpenseEntry -> state.copy(
                    type = EntryType.Expense,
                    amountText = formatAmount(entry.amount),
                    categoryId = entry.category.id,
                    accountId = entry.account.id,
                    toAccountId = null,
                    toAmountText = "",
                    description = entry.description.orEmpty(),
                )

                is IncomeEntry -> state.copy(
                    type = EntryType.Income,
                    amountText = formatAmount(entry.amount),
                    categoryId = entry.category.id,
                    accountId = entry.account.id,
                    toAccountId = null,
                    toAmountText = "",
                    description = entry.description.orEmpty(),
                )
            }
        }
    }

    private val _selectedType = MutableStateFlow(EntryType.Expense)
    val selectedType: StateFlow<EntryType> = _selectedType.asStateFlow()

    /** Switches the entry type, carrying over the fields shared across types. */
    fun selectType(type: EntryType) {
        if (type == _selectedType.value) return
        _selectedType.value = type
        viewModelScope.launch {
            val previous = draftState.value
            val fresh = newEntryState(type)
            draftState.value = if (previous != null && previous.type != type) {
                fresh.carryOverCommonFields(previous)
            } else {
                fresh
            }
        }
    }

    /** Seeds the draft from the latest entry of the selected type, unless one already exists. */
    fun seedDraft() {
        viewModelScope.launch {
            if (draftState.value == null) {
                draftState.value = newEntryState(_selectedType.value)
            }
        }
    }

    /** Resets the form to an empty draft of the selected type. */
    fun clearDraft() {
        draftState.value = EntryDraft.forNew(_selectedType.value)
    }

    /** Saves the current draft and resets the form prefilled from the saved entry. */
    fun saveDraft(): Boolean {
        val state = draftState.value ?: return false
        val entry = state.toEntry(accounts.value, categories.value) ?: return false
        createEntry(entry)
        draftState.value = EntryDraft.forNew(entry.type, entry)
        return true
    }
}
