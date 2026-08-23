package org.sjbtimdan.linden.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.predictions.FieldPredictionInput
import org.sjbtimdan.linden.predictions.PREDICTION_TOP_N
import org.sjbtimdan.linden.predictions.predictAccounts
import org.sjbtimdan.linden.predictions.predictCategories
import kotlin.time.Clock
import kotlin.time.Instant

abstract class EntryEditorViewModel(
    private val entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
) : ViewModel() {
    val accounts: StateFlow<List<Account>> = accountDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    val categories: StateFlow<List<Category>> = categoryDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    val allEntries: StateFlow<List<Entry>> = entryDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    /** In-progress entry being created or edited, or null when no editor is shown. */
    protected val draftState = MutableStateFlow<EntryDraft?>(null)
    val draft: StateFlow<EntryDraft?> = draftState.asStateFlow()

    /**
     * Most likely account ids for the current draft, recomputed whenever it
     * changes. Only applies to new entries; editing a draft is never predicted.
     */
    val accountSuggestions: StateFlow<List<Long>> = combine(draft, allEntries) { state, entries ->
        if (state == null || state.editing != null) {
            emptyList()
        } else {
            predictAccounts(
                entries = entries,
                input = FieldPredictionInput(
                    type = state.type,
                    categoryId = state.categoryId,
                    accountId = state.accountId,
                    amount = state.amount,
                    description = state.description,
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

    /**
     * Most likely category ids for the current draft, recomputed whenever it
     * changes. Only applies to new entries; editing a draft is never predicted.
     */
    val categorySuggestions: StateFlow<List<Long>> = combine(draft, allEntries) { state, entries ->
        if (state == null || state.editing != null) {
            emptyList()
        } else {
            predictCategories(
                entries = entries,
                input = FieldPredictionInput(
                    type = state.type,
                    categoryId = state.categoryId,
                    accountId = state.accountId,
                    amount = state.amount,
                    description = state.description,
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

    fun onAmountChange(text: String) = draftState.update { it?.copy(amountText = text) }
    fun onCategoryChange(id: Long?) = draftState.update { it?.copy(categoryId = id) }
    fun onAccountChange(id: Long?) = draftState.update { it?.copy(accountId = id) }
    fun onToAccountChange(id: Long?) = draftState.update { it?.copy(toAccountId = id) }
    fun onToAmountChange(text: String) = draftState.update { it?.copy(toAmountText = text) }
    fun onDescriptionChange(text: String) = draftState.update { it?.copy(description = text) }
    fun onCreatedAtChange(instant: Instant) = draftState.update { it?.copy(createdAt = instant) }

    fun createEntry(entry: Entry) {
        viewModelScope.launch {
            entryDao.create(entry)
        }
    }

    fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            entryDao.update(entry)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            entryDao.delete(id)
        }
    }

    suspend fun newEntryState(type: EntryType): EntryDraft = EntryDraft.forNew(type, entryDao.latest(type))
}
