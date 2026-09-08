package org.sjbtimdan.linden.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.HideEntryTotalSetting
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Budget
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.FxRate
import kotlin.time.Clock

/** Number of months in the trend window and of bars in the chart. */
internal const val WINDOW_MONTHS = 12

/**
 * Monthly expense and income totals for the insights screen. The rolling
 * 12-month window can be paged backwards (and forwards again, up to the
 * current month) via [stepBack]/[stepForward]; "now" is re-read on every
 * recomputation, so the current-month marker and the forward limit follow the
 * calendar without any timer. [selectedIndex] picks the inspected month —
 * [-1] (and paging) selects the window's last month — and [breakdown]
 * carries that month's per-category rows.
 */
class InsightsViewModel(
    private val entryDao: EntryDao,
    settingsDao: SettingsDao,
    private val ratesProvider: RatesFlowProvider,
    budgetDao: BudgetDao,
    allEntries: StateFlow<List<Entry>>,
    initialHideEntryTotal: Boolean = false,
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : ViewModel() {
    /** The currency totals are displayed in, from the settings. */
    val defaultCurrency: StateFlow<Currency> = ratesProvider.defaultCurrency

    /** Whether totals are masked by the "Hide totals" setting. */
    val hideTotal: StateFlow<Boolean> =
        HideEntryTotalSetting(settingsDao, initialHideEntryTotal, viewModelScope).state

    /** Whether any entry exists at all — drives the screen's no-data hint. */
    val hasEntries: StateFlow<Boolean> = entryDao.entryExists().stateFlow(false)

    /** First day of the month the 12-month window ends with. */
    private val _windowEnd = MutableStateFlow(currentMonthStart(today()))
    val windowEnd: StateFlow<LocalDate> = _windowEnd.asStateFlow()

    /** Inspected month's index within the window; -1 means the window's last month. */
    private val _selectedIndex = MutableStateFlow(-1)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    /** 12-month expense/income window ending with [windowEnd]. */
    val months: StateFlow<List<MonthTotal>> = combine(
        allEntries,
        ratesProvider.defaultCurrency,
        ratesProvider.rates,
        _windowEnd,
    ) { entries, currency, rates, windowEnd ->
        monthlyTotals(entries, windowEnd, currentMonthStart(today()), currency, rates)
    }.stateFlow(emptyList())

    /** False once the window already ends with the current month. */
    val canStepForward: StateFlow<Boolean> = _windowEnd
        .map { end -> monthIndexOf(end) < monthIndexOf(currentMonthStart(today())) }
        .stateFlow(true)

    /** Category rows of the selected month (window-end month when unselected). */
    val breakdown: StateFlow<MonthBreakdown?> = combine(
        allEntries,
        ratesProvider.defaultCurrency,
        ratesProvider.rates,
        budgetDao.budgetsFlow(),
        _windowEnd,
    ) { entries, currency, rates, budgets, windowEnd ->
        BreakdownInput(entries, currency, rates, budgets, windowEnd)
    }.combine(_selectedIndex) { input, selected ->
        val index = if (selected == -1 || selected >= WINDOW_MONTHS) WINDOW_MONTHS - 1 else selected
        val month = monthBefore(input.windowEnd, WINDOW_MONTHS - 1 - index)
        categoryBreakdown(input.entries, month, input.currency, input.rates, input.budgets)
    }.stateFlow(null)

    /** Inspects the month at [index] within the window. */
    fun selectMonth(index: Int) {
        if (index in 0 until WINDOW_MONTHS) _selectedIndex.value = index
    }

    /** Pages the window twelve months backwards, back to the new window's end. */
    fun stepBack() {
        _windowEnd.update { it.minus(WINDOW_MONTHS, DateTimeUnit.MONTH) }
        _selectedIndex.value = -1
    }

    /** Pages the window twelve months forwards, never past the current month. */
    fun stepForward() {
        val now = currentMonthStart(today())
        _windowEnd.update { end ->
            val stepped = end.plus(WINDOW_MONTHS, DateTimeUnit.MONTH)
            if (monthIndexOf(stepped) <= monthIndexOf(now)) stepped else end
        }
        _selectedIndex.value = -1
    }

    private fun <T> Flow<T>.stateFlow(initial: T): StateFlow<T> = stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initial,
    )
}

private fun currentMonthStart(date: LocalDate): LocalDate = LocalDate(date.year, date.month.number, 1)

private data class BreakdownInput(
    val entries: List<Entry>,
    val currency: Currency,
    val rates: List<FxRate>,
    val budgets: List<Budget>,
    val windowEnd: LocalDate,
)
