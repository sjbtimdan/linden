package org.sjbtimdan.linden.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry

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
    protected val _draft = MutableStateFlow<EntryDraft?>(null)
    val draft: StateFlow<EntryDraft?> = _draft.asStateFlow()

    fun onAmountChange(text: String) = _draft.update { it?.copy(amountText = text) }
    fun onCategoryChange(id: Long?) = _draft.update { it?.copy(categoryId = id) }
    fun onAccountChange(id: Long?) = _draft.update { it?.copy(accountId = id) }
    fun onToAccountChange(id: Long?) = _draft.update { it?.copy(toAccountId = id) }
    fun onToAmountChange(text: String) = _draft.update { it?.copy(toAmountText = text) }
    fun onDescriptionChange(text: String) = _draft.update { it?.copy(description = text) }
    fun onCreatedAtChange(instant: Instant) = _draft.update { it?.copy(createdAt = instant) }

    fun createEntry(entry: Entry) {
        viewModelScope.launch {
            when (entry) {
                is ExpenseEntry -> entryDao.create(entry)
                is IncomeEntry -> entryDao.create(entry)
                is TransferEntry -> entryDao.create(entry)
            }
        }
    }

    fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            when (entry) {
                is ExpenseEntry -> entryDao.update(entry)
                is IncomeEntry -> entryDao.update(entry)
                is TransferEntry -> entryDao.update(entry)
            }
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            entryDao.delete(id)
        }
    }

    suspend fun newEntryState(type: EntryType): EntryDraft =
        EntryDraft.forNew(type, entryDao.latest(type))
}
