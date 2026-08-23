package org.sjbtimdan.linden.ui.ledger

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.entry.EntryDraft
import org.sjbtimdan.linden.ui.entry.EntryEditorViewModel
import org.sjbtimdan.linden.ui.entry.EntrySuggestionsProvider

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
