package org.sjbtimdan.linden.ui.insights

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.CpiDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.HideEntryTotalSetting
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.inflation.CpiSeries
import org.sjbtimdan.linden.inflation.EntryInflation
import org.sjbtimdan.linden.model.Budget
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.time.AppClock
import org.sjbtimdan.linden.ui.AppViewModel

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
    cpiDao: CpiDao,
    allEntries: StateFlow<List<Entry>>,
    initialHideEntryTotal: Boolean = false,
    private val clock: AppClock,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : AppViewModel() {
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

    /** Restate displayed amounts in the latest stored month's money. */
    private val _inflationAdjusted = MutableStateFlow(false)
    val inflationAdjusted: StateFlow<Boolean> = _inflationAdjusted.asStateFlow()

    private val cpiSeries: StateFlow<Map<Currency, CpiSeries>> = cpiDao.seriesFlow().stateFlow(emptyMap())

    /** Whether any CPI data is stored; drives the toggle. */
    val inflationAvailable: StateFlow<Boolean> = cpiSeries.map { it.isNotEmpty() }.stateFlow(false)

    /** Latest stored month across currencies, for the toggle's caption. */
    val inflationThroughMonth: StateFlow<String?> =
        cpiSeries.map { byCurrency -> byCurrency.values.maxOfOrNull(CpiSeries::latestMonth) }.stateFlow(null)

    private val inflationAdjustment: StateFlow<EntryInflation> =
        combine(_inflationAdjusted, cpiSeries) { adjusted, series -> EntryInflation(adjusted, series, zone) }
            .stateFlow(EntryInflation(false, emptyMap(), zone))

    /** [allEntries] restated in the latest stored month's money while the toggle is on. */
    private val adjustedEntries: StateFlow<List<Entry>> =
        combine(allEntries, inflationAdjustment) { entries, adjustment -> adjustment.adjustedEntries(entries) }
            .stateFlow(emptyList())

    /** 12-month expense/income window ending with [windowEnd]. */
    val months: StateFlow<List<MonthTotal>> = combine(
        adjustedEntries,
        ratesProvider.defaultCurrency,
        ratesProvider.rates,
        _windowEnd,
    ) { entries, currency, rates, windowEnd ->
        monthlyTotals(entries, windowEnd, currentMonthStart(today()), currency, rates, zone)
    }.stateFlow(emptyList())

    /** False once the window already ends with the current month. */
    val canStepForward: StateFlow<Boolean> = _windowEnd
        .map { end -> monthIndexOf(end) < monthIndexOf(currentMonthStart(today())) }
        .stateFlow(true)

    /** Category rows of the selected month (window-end month when unselected). */
    val breakdown: StateFlow<MonthBreakdown?> = combine(
        adjustedEntries,
        ratesProvider.defaultCurrency,
        ratesProvider.rates,
        budgetDao.budgetsFlow(),
        _windowEnd,
    ) { entries, currency, rates, budgets, windowEnd ->
        BreakdownInput(entries, currency, rates, budgets, windowEnd)
    }.combine(_selectedIndex) { input, selected ->
        val index = if (selected == -1 || selected >= WINDOW_MONTHS) WINDOW_MONTHS - 1 else selected
        val month = monthBefore(input.windowEnd, WINDOW_MONTHS - 1 - index)
        categoryBreakdown(input.entries, month, input.currency, input.rates, input.budgets, zone)
    }.stateFlow(null)

    /** Inspects the month at [index] within the window. */
    fun selectMonth(index: Int) {
        if (index in 0 until WINDOW_MONTHS) _selectedIndex.value = index
    }

    /** Switches the trend and breakdown between nominal and the latest month's money. */
    fun setInflationAdjusted(adjusted: Boolean) {
        _inflationAdjusted.value = adjusted
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

    private fun today(): LocalDate = clock.todayIn(zone)
}

private fun currentMonthStart(date: LocalDate): LocalDate = LocalDate(date.year, date.month.number, 1)

private data class BreakdownInput(
    val entries: List<Entry>,
    val currency: Currency,
    val rates: List<FxRate>,
    val budgets: List<Budget>,
    val windowEnd: LocalDate,
)
