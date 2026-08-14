package org.sjbtimdan.linden.ui.rates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToLong

@Composable
fun RatesScreen(
    viewModel: RatesViewModel,
    onNavigateBack: () -> Unit,
) {
    val base by viewModel.base.collectAsState()
    val rates by viewModel.rates.collectAsState()
    val ratesRefreshState by viewModel.ratesRefreshState.collectAsState()

    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .padding(16.dp)
            .widthIn(max = 480.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Currency rates",
                fontSize = 28.sp,
            )
            TextButton(onClick = onNavigateBack) {
                Text("< Settings")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "1 ${base.symbol}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                rates.firstOrNull()?.date?.let { date ->
                    Text(
                        text = "Rates from $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilledTonalButton(
                onClick = viewModel::refreshRates,
                enabled = ratesRefreshState !is RatesRefreshState.Refreshing,
            ) {
                if (ratesRefreshState is RatesRefreshState.Refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Refresh")
                }
            }
        }

        when (val state = ratesRefreshState) {
            RatesRefreshState.Idle -> Unit

            RatesRefreshState.Refreshing -> Unit

            is RatesRefreshState.Error -> ErrorRow(
                text = "Refresh failed: ${state.message}",
                onDismiss = viewModel::clearRatesError,
            )
        }

        if (rates.isEmpty() && ratesRefreshState !is RatesRefreshState.Refreshing) {
            Text(
                text = "No rates loaded yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            rates.sortedBy { it.quoteCurrency.name }.forEach { rate ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "1 ${rate.baseCurrency.symbol} = ${formatRate(rate.rate)} ${rate.quoteCurrency.symbol}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorRow(
    text: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onDismiss,
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Dismiss",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

internal fun formatRate(rate: Double): String {
    val rounded = (rate * 10_000).roundToLong() / 10_000.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
