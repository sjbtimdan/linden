package org.sjbtimdan.linden.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.model.Budget
import org.sjbtimdan.linden.model.Category

class BudgetViewModel(
    private val budgetDao: BudgetDao,
    categoryDao: CategoryDao,
) : ViewModel() {
    val budgets: StateFlow<List<Budget>> = budgetDao.budgetsFlow()
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

    /** Adds or updates a budget for [categoryName]; returns false when the limit is not positive. */
    fun saveBudget(categoryName: String, limitMinor: Long): Boolean {
        val name = categoryName.trim()
        if (name.isEmpty() || limitMinor <= 0) return false
        viewModelScope.launch {
            budgetDao.upsert(name, limitMinor)
        }
        return true
    }

    /** Removes the budget for [categoryName] if one exists. */
    fun deleteBudget(categoryName: String) {
        viewModelScope.launch {
            budgetDao.delete(categoryName)
        }
    }
}
