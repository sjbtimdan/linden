package org.sjbtimdan.linden.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.first_run_continue
import org.sjbtimdan.linden.resources.first_run_subtitle
import org.sjbtimdan.linden.resources.first_run_title

/**
 * Shown once on a fresh install, before the app: asks for the default currency
 * so the starter accounts are seeded in a currency the user recognizes. The
 * app's existing default (CHF) is pre-selected so Continue is always enabled.
 */
@Composable
fun FirstRunScreen(onComplete: (Currency) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(Currency.CHF) }

    Column(
        modifier = Modifier
            .screenInsets()
            .fillMaxSize()
            .padding(ScreenPadding)
            .widthIn(max = ScreenMaxWidth)
            .testTag("firstRunScreen"),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.first_run_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.first_run_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Currency.entries.forEach { currency ->
                FilterChip(
                    selected = selected == currency,
                    onClick = { selected = currency },
                    label = { Text(currency.name) },
                    modifier = Modifier.testTag("firstRunCurrency-${currency.name}"),
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onComplete(selected) },
            modifier = Modifier
                .align(Alignment.End)
                .testTag("firstRunContinue"),
        ) {
            Text(stringResource(Res.string.first_run_continue))
        }
    }
}
