package org.sjbtimdan.linden.ui.ledger

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.predictions.DescriptionPredictionInput
import org.sjbtimdan.linden.predictions.PREDICTION_TOP_N
import org.sjbtimdan.linden.predictions.predictDescriptions
import org.sjbtimdan.linden.ui.entry.EntryDraft
import org.sjbtimdan.linden.ui.entry.EntryEditorViewModel
import kotlin.time.Clock

class LedgerViewModel(
    entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
) : EntryEditorViewModel(entryDao, accountDao, categoryDao) {
    private val _selectedType = MutableStateFlow(EntryType.Expense)
    val selectedType: StateFlow<EntryType> = _selectedType.asStateFlow()

    /** Description suggestions for the current draft, recomputed whenever it changes. */
    val descriptionSuggestions: StateFlow<List<String>> = combine(draft, allEntries) { state, entries ->
        if (state == null || state.editing != null) {
            emptyList()
        } else {
            predictDescriptions(
                entries = entries,
                input = DescriptionPredictionInput(
                    type = state.type,
                    categoryId = state.categoryId,
                    accountId = state.accountId,
                    amount = state.amount,
                ),
                now = Clock.System.now(),
                timeZone = TimeZone.currentSystemDefault(),
                topN = PREDICTION_TOP_N,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

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
