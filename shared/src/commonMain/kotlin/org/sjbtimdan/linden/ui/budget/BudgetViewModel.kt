package org.sjbtimdan.linden.ui.budget

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.HideEntryTotalSetting
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Budget
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.ui.AppViewModel

class BudgetViewModel(
    private val budgetDao: BudgetDao,
    categoryDao: CategoryDao,
    settingsDao: SettingsDao,
    initialHideEntryTotal: Boolean = false,
) : AppViewModel() {
    val budgets: StateFlow<List<Budget>> = budgetDao.budgetsFlow().stateFlow(emptyList())

    val categories: StateFlow<List<Category>> = categoryDao.getAll().stateFlow(emptyList())

    /** Whether amounts are masked by the "Hide totals" setting. */
    val hideTotal: StateFlow<Boolean> =
        HideEntryTotalSetting(settingsDao, initialHideEntryTotal, viewModelScope).state

    /** Adds or updates a budget for [categoryName]; returns false when the limit is not positive. */
    fun saveBudget(categoryName: String, limitMinor: Long): Boolean {
        val name = categoryName.trim()
        if (name.isEmpty() || limitMinor <= 0) return false
        launchWrite { budgetDao.upsert(name, limitMinor) }
        return true
    }

    /** Removes the budget for [categoryName] if one exists. */
    fun deleteBudget(categoryName: String) {
        launchWrite { budgetDao.delete(categoryName) }
    }
}
