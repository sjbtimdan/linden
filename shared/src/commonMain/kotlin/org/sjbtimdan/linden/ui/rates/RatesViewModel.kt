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
import kotlinx.coroutines.flow.update
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
    private val settingsDao: SettingsDao,
    private val fxRatesRepository: FxRatesRepository,
) : ViewModel() {
    val base: StateFlow<Currency> = settingsDao.defaultCurrencyFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Currency.CHF,
        )

    val autoUpdateRates: StateFlow<Boolean> = settingsDao.autoUpdateRatesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true,
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
                if (autoUpdateRates.value) refresh(currency)
            }
        }
    }

    fun refreshRates(currency: Currency = base.value) {
        refresh(currency)
    }

    fun refreshRatesIfStale(currency: Currency = base.value) {
        viewModelScope.launch {
            if (!autoUpdateRates.value) return@launch
            try {
                fxRatesRepository.refreshIfStale(currency)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ratesRefreshState.update { RatesRefreshState.Error(e.message ?: "Failed to refresh rates") }
            }
        }
    }

    fun setAutoUpdateRates(enabled: Boolean) {
        viewModelScope.launch { settingsDao.setAutoUpdateRates(enabled) }
    }

    fun setRate(quote: Currency, rate: Double) {
        viewModelScope.launch { fxRatesRepository.setRate(base.value, quote, rate) }
    }

    fun clearRatesError() {
        _ratesRefreshState.update { RatesRefreshState.Idle }
    }

    private fun refresh(currency: Currency) {
        viewModelScope.launch {
            _ratesRefreshState.update { RatesRefreshState.Refreshing }
            _ratesRefreshState.update {
                try {
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
}
