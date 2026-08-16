package org.sjbtimdan.linden.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.history.sumInDefaultMinor

class CategoryListViewModel(
    private val categoryDao: CategoryDao,
    entryDao: EntryDao,
    settingsDao: SettingsDao,
    fxRatesRepository: FxRatesRepository,
) : ViewModel() {
    private val ratesFlow = RatesFlowProvider(settingsDao, fxRatesRepository, viewModelScope)

    private val totals: StateFlow<Map<Pair<Long, Currency>, Long>> = entryDao.categoryTotals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap(),
        )

    val defaultCurrency: StateFlow<Currency> = ratesFlow.defaultCurrency

    val categories: StateFlow<List<CategoryWithBalance>> = combine(
        categoryDao.getAll(),
        totals,
        ratesFlow.defaultCurrency,
        ratesFlow.rates,
    ) { categories, totals, currency, rates ->
        categories.map { category ->
            CategoryWithBalance(category, categoryBalanceMinor(totals, category.id, currency, rates))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    /** Net total of all entries in the default currency; null when a rate is missing. */
    val totalMinor: StateFlow<Long?> = combine(
        totals,
        ratesFlow.defaultCurrency,
        ratesFlow.rates,
    ) { totals, currency, rates ->
        sumInDefaultMinor(totals.map { (key, net) -> key.second to net }, currency, rates)
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
