package org.sjbtimdan.linden.ui.rates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.ScreenMaxWidth
import org.sjbtimdan.linden.ui.ScreenPadding
import org.sjbtimdan.linden.ui.screenInsets

@Composable
fun RatesScreen(viewModel: RatesViewModel, onNavigateBack: () -> Unit) {
    val base by viewModel.base.collectAsState()
    val rates by viewModel.rates.collectAsState()
    val ratesRefreshState by viewModel.ratesRefreshState.collectAsState()
    val autoUpdateRates by viewModel.autoUpdateRates.collectAsState()
    var editingQuote by remember { mutableStateOf<Currency?>(null) }
    val rateByQuote = rates.associateBy { it.quoteCurrency }

    Column(
        modifier = Modifier
            .screenInsets()
            .fillMaxSize()
            .padding(ScreenPadding)
            .widthIn(max = ScreenMaxWidth)
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
            )
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
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Update automatically",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Downloads fresh rates when the cached ones are more than a day old. " +
                        "Turn off to keep manually entered rates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = autoUpdateRates,
                onCheckedChange = viewModel::setAutoUpdateRates,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Currency.entries.filter { it != base }.sortedBy { it.name }.forEach { quote ->
            RateRow(
                base = base,
                quote = quote,
                rate = rateByQuote[quote]?.rate,
                onEdit = { editingQuote = quote },
            )
        }
    }

    editingQuote?.let { quote ->
        RateEditorDialog(
            quoteCurrency = quote,
            currentRate = rateByQuote[quote]?.rate,
            onSave = { rate ->
                viewModel.setRate(quote, rate)
                editingQuote = null
            },
            onDismiss = { editingQuote = null },
        )
    }
}

@Composable
private fun RateRow(base: Currency, quote: Currency, rate: Double?, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = quote.symbol,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = rateRowLabel(base, quote, rate),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        IconButton(
            onClick = onEdit,
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit ${quote.name} rate",
            )
        }
    }
}

@Composable
private fun ErrorRow(text: String, onDismiss: () -> Unit) {
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
