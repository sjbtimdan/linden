package org.sjbtimdan.linden.ui.ledger

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.util.stateFlow

/**
 * The ledger's input filters: search text, type/amount/category/account
 * narrowing, the shown period and the view mode. Kept separate from the
 * aggregation flows in [LedgerViewModel] so the filter rules can be read and
 * tested on their own.
 */
internal class LedgerFilterState(
    scope: CoroutineScope,
    initialAnchor: LocalDate,
) {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<EntryType?>(null)
    val typeFilter: StateFlow<EntryType?> = _typeFilter.asStateFlow()

    private val _amountFilter = MutableStateFlow<AmountFilter?>(null)
    val amountFilter: StateFlow<AmountFilter?> = _amountFilter.asStateFlow()

    /** Show future-dated entries in every view mode. */
    private val _showFuture = MutableStateFlow(false)
    val showFuture: StateFlow<Boolean> = _showFuture.asStateFlow()

    private val _periodSelection = MutableStateFlow(PeriodSelection(LedgerPeriod.Month, initialAnchor))
    val periodSelection: StateFlow<PeriodSelection> = _periodSelection.asStateFlow()

    /** Selected period's start/end, derived once and shared by every period flow. */
    val periodWindow: StateFlow<PeriodWindow?> = _periodSelection
        .map { selection ->
            val start = selection.period.windowStart(selection.anchor) ?: return@map null
            val end = selection.period.windowEnd(selection.anchor) ?: return@map null
            PeriodWindow(start, end)
        }
        .stateFlow(scope, null)

    private val _viewMode = MutableStateFlow(LedgerViewMode.Entries)
    val viewMode: StateFlow<LedgerViewMode> = _viewMode.asStateFlow()

    /** Category filter id; 0 means "Uncategorized". */
    private val _categoryFilter = MutableStateFlow<Long?>(null)
    val categoryFilter: StateFlow<Long?> = _categoryFilter.asStateFlow()

    private val _accountFilter = MutableStateFlow<Long?>(null)
    val accountFilter: StateFlow<Long?> = _accountFilter.asStateFlow()

    /** The type filter as the categories view applies it: Transfer counts as All. */
    val typeFilterForCategories: StateFlow<EntryType?> = _typeFilter
        .map { type -> type?.takeUnless { it == EntryType.Transfer } }
        .stateFlow(scope, null)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: EntryType?) {
        _typeFilter.value = type
    }

    /** Sets the amount filter on the entries view; null clears it. */
    fun setAmountFilter(filter: AmountFilter?) {
        _amountFilter.value = filter
    }

    /** Removes the amount filter, restoring all amounts. */
    fun clearAmountFilter() {
        _amountFilter.value = null
    }

    fun setShowFuture(show: Boolean) {
        _showFuture.value = show
    }

    fun setPeriod(period: LedgerPeriod) {
        _periodSelection.update { it.copy(period = period) }
    }

    fun setViewMode(mode: LedgerViewMode) {
        // The entry filters only apply to the entries view; leaving it clears
        // them so no stale chip can sit over the categories or accounts list.
        if (mode != LedgerViewMode.Entries) {
            _categoryFilter.value = null
            _accountFilter.value = null
        }
        _viewMode.value = mode
    }

    /** Drills into a category from the categories view: narrows the entries to it and switches to the entries view. */
    fun openCategory(categoryId: Long) {
        setCategoryFilter(categoryId)
        _viewMode.value = LedgerViewMode.Entries
    }

    /** Drills into an account from the accounts view: narrows the entries to it and switches to the entries view. */
    fun openAccount(accountId: Long) {
        setAccountFilter(accountId)
        _viewMode.value = LedgerViewMode.Entries
    }

    /** Narrows the entries view to a single category; null restores all entries. */
    fun setCategoryFilter(categoryId: Long?) {
        _categoryFilter.value = categoryId
    }

    /** Removes the category filter, restoring the full entries list. */
    fun clearCategoryFilter() {
        _categoryFilter.value = null
    }

    /** Narrows the entries view to a single account; null restores all entries. */
    fun setAccountFilter(accountId: Long?) {
        _accountFilter.value = accountId
    }

    /** Removes the account filter, restoring the full entries list. */
    fun clearAccountFilter() {
        _accountFilter.value = null
    }

    fun goToPreviousPeriod() {
        _periodSelection.update { it.copy(anchor = it.period.previousAnchor(it.anchor)) }
    }

    fun goToNextPeriod() {
        _periodSelection.update { it.copy(anchor = it.period.nextAnchor(it.anchor)) }
    }
}
