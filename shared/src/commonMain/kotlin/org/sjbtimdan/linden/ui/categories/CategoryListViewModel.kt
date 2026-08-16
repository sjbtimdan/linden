package org.sjbtimdan.linden.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.ui.history.periodTotalMinor

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryListViewModel(
    private val categoryDao: CategoryDao,
    entryDao: EntryDao,
    settingsDao: SettingsDao,
    fxRatesRepository: FxRatesRepository,
) : ViewModel() {
    private val entries: StateFlow<List<Entry>> = entryDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    val defaultCurrency: StateFlow<Currency> = settingsDao.defaultCurrencyFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Currency.CHF,
        )

    private val rates: StateFlow<List<FxRate>> = defaultCurrency
        .flatMapLatest { fxRatesRepository.ratesFor(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    val categories: StateFlow<List<CategoryWithBalance>> = combine(
        categoryDao.getAll(),
        entries,
        defaultCurrency,
        rates,
    ) { categories, entries, currency, rates ->
        categories.map { category ->
            CategoryWithBalance(category, categoryBalanceMinor(entries, category, currency, rates))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    /** Net total of all entries in the default currency; null when a rate is missing. */
    val totalMinor: StateFlow<Long?> = combine(
        entries,
        defaultCurrency,
        rates,
    ) { entries, currency, rates ->
        periodTotalMinor(entries, currency, rates)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    fun createCategory(name: String, type: CategoryType) {
        viewModelScope.launch {
            categoryDao.create(name, type)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.update(category)
        }
    }
}
