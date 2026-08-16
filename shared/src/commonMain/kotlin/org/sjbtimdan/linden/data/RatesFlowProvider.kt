package org.sjbtimdan.linden.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate

/**
 * Exposes the stored default currency and its FX rates as state flows, re-emitting
 * whenever either the currency or the stored rates change.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RatesFlowProvider(
    settingsDao: SettingsDao,
    fxRatesRepository: FxRatesRepository,
    scope: CoroutineScope,
) {
    val defaultCurrency: StateFlow<Currency> = settingsDao.defaultCurrencyFlow()
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = Currency.CHF)

    val rates: StateFlow<List<FxRate>> = defaultCurrency
        .flatMapLatest { currency -> fxRatesRepository.ratesFor(currency) }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())
}
