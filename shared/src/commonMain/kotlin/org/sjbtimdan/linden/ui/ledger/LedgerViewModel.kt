package org.sjbtimdan.linden.ui.ledger

import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.ui.entry.EntryEditorViewModel

class LedgerViewModel(
    entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
) : EntryEditorViewModel(entryDao, accountDao, categoryDao) {
    val recentEntries: StateFlow<List<Entry>> = entryDao.getSince(
        (Clock.System.now() - 7.days).toEpochMilliseconds(),
    ).map { entries ->
        // Bounds are checked at emission time so entries created after this
        // ViewModel was instantiated are not hidden behind a stale "now".
        entries.filterNot { it.createdAt > Clock.System.now() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )
}
