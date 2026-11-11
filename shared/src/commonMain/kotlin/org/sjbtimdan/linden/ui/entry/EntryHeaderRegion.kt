package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.ui.rates.RatesWarning
import org.sjbtimdan.linden.ui.rates.RatesWarningBanner

@Composable
internal fun EntryHeaderRegion(
    viewModel: EntryPointViewModel,
    ratesWarning: RatesWarning?,
    compact: Boolean,
    onSetRates: () -> Unit,
    onNavigateToInsights: () -> Unit,
) {
    val hasEntries by viewModel.hasEntries.collectAsState()
    val totalMinor by viewModel.totalMinor.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val hideTotal by viewModel.hideTotal.collectAsState()
    val showRatesWarning by viewModel.showRatesWarning.collectAsState()

    // The hero card is dead weight before the first entry exists.
    if (hasEntries) {
        TotalBalanceCard(
            total = totalMinor,
            currency = defaultCurrency,
            hidden = hideTotal,
            compact = compact,
            onToggleHidden = { viewModel.setHideTotal(!hideTotal) },
            onNavigateToInsights = onNavigateToInsights,
        )
    }

    // FX rates only matter once entries span currencies or the user has
    // visited the Rates screen; until then the warning is day-one noise.
    if (showRatesWarning) {
        ratesWarning?.let { warning ->
            Spacer(modifier = Modifier.height(12.dp))

            RatesWarningBanner(
                warning = warning,
                onSetRates = onSetRates,
                compact = compact,
            )
        }
    }
}
