package org.sjbtimdan.linden.ui.rates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate

sealed interface RatesRefreshState {
    data object Idle : RatesRefreshState
    data object Refreshing : RatesRefreshState
    data class Error(val message: String) : RatesRefreshState
}

class RatesViewModel(
    settingsDao: SettingsDao,
    private val fxRatesRepository: FxRatesRepository,
) : ViewModel() {
    val base: StateFlow<Currency> = settingsDao.defaultCurrencyFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Currency.CHF,
        )

    private val _rates = MutableStateFlow<List<FxRate>>(emptyList())
    val rates: StateFlow<List<FxRate>> = _rates.asStateFlow()

    private val _ratesRefreshState = MutableStateFlow<RatesRefreshState>(RatesRefreshState.Idle)
    val ratesRefreshState: StateFlow<RatesRefreshState> = _ratesRefreshState.asStateFlow()

    init {
        viewModelScope.launch {
            base.collectLatest { currency ->
                fxRatesRepository.ratesFor(currency).collect { _rates.value = it }
            }
        }
        viewModelScope.launch {
            base.drop(1).collectLatest { currency ->
                refresh(currency)
            }
        }
    }

    fun refreshRates(currency: Currency = base.value) {
        refresh(currency)
    }

    fun clearRatesError() {
        _ratesRefreshState.value = RatesRefreshState.Idle
    }

    private fun refresh(currency: Currency) {
        viewModelScope.launch {
            _ratesRefreshState.value = RatesRefreshState.Refreshing
            _ratesRefreshState.value = try {
                fxRatesRepository.refreshRates(currency)
                RatesRefreshState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                RatesRefreshState.Error(e.message ?: "Failed to refresh rates")
            }
        }
    }
}
