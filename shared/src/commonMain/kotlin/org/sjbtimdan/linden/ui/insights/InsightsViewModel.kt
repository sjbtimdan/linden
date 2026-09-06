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
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.HideEntryTotalSetting
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Currency
import kotlin.time.Clock

/**
 * Monthly expense and income totals for the insights screen. The rolling
 * 12-month window can be paged backwards (and forwards again, up to the
 * current month) via [stepBack]/[stepForward]; "now" is re-read on every
 * recomputation, so the current-month marker and the forward limit follow the
 * calendar without any timer.
 */
class InsightsViewModel(
    entryDao: EntryDao,
    settingsDao: SettingsDao,
    fxRatesRepository: FxRatesRepository,
    initialHideEntryTotal: Boolean = false,
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : ViewModel() {
    private val ratesFlow = RatesFlowProvider(settingsDao, fxRatesRepository, viewModelScope)

    /** The currency totals are displayed in, from the settings. */
    val defaultCurrency: StateFlow<Currency> = ratesFlow.defaultCurrency

    /** Whether totals are masked by the "Hide totals" setting. */
    val hideTotal: StateFlow<Boolean> =
        HideEntryTotalSetting(settingsDao, initialHideEntryTotal, viewModelScope).state

    /** First day of the month the 12-month window ends with. */
    private val _windowEnd = MutableStateFlow(currentMonthStart(today()))
    val windowEnd: StateFlow<LocalDate> = _windowEnd.asStateFlow()

    /** 12-month expense/income window ending with [windowEnd]. */
    val months: StateFlow<List<MonthTotal>> = combine(
        entryDao.getAll(),
        ratesFlow.defaultCurrency,
        ratesFlow.rates,
        _windowEnd,
    ) { entries, currency, rates, windowEnd ->
        monthlyTotals(entries, windowEnd, currentMonthStart(today()), currency, rates)
    }.stateFlow(emptyList())

    /** False once the window already ends with the current month. */
    val canStepForward: StateFlow<Boolean> = _windowEnd
        .map { end -> monthIndexOf(end) < monthIndexOf(currentMonthStart(today())) }
        .stateFlow(true)

    /** Pages the window twelve months backwards. */
    fun stepBack() = _windowEnd.update { it.minus(12, DateTimeUnit.MONTH) }

    /** Pages the window twelve months forwards, never past the current month. */
    fun stepForward() {
        val now = currentMonthStart(today())
        _windowEnd.update { end ->
            val stepped = end.plus(12, DateTimeUnit.MONTH)
            if (monthIndexOf(stepped) <= monthIndexOf(now)) stepped else end
        }
    }

    private fun <T> Flow<T>.stateFlow(initial: T): StateFlow<T> = stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initial,
    )
}

private fun currentMonthStart(date: LocalDate): LocalDate = LocalDate(date.year, date.month.number, 1)
