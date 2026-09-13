package org.sjbtimdan.linden.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.util.stateFlow

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
    val defaultCurrency: StateFlow<Currency> =
        settingsDao.defaultCurrencyFlow().stateFlow(scope, Currency.CHF)

    val rates: StateFlow<List<FxRate>> = defaultCurrency
        .flatMapLatest { currency -> fxRatesRepository.ratesFor(currency) }
        .stateFlow(scope, emptyList())
}
