package org.sjbtimdan.linden.ui.inflation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.inflation.CpiSeedData
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.inflation_change_caption
import org.sjbtimdan.linden.resources.inflation_empty
import org.sjbtimdan.linden.resources.inflation_source
import org.sjbtimdan.linden.resources.inflation_through
import org.sjbtimdan.linden.resources.inflation_title
import org.sjbtimdan.linden.ui.ScreenBackButton
import org.sjbtimdan.linden.ui.screenContainer

@Composable
fun InflationScreen(viewModel: InflationViewModel, onNavigateBack: () -> Unit) {
    val rows by viewModel.rows.collectAsState()

    Column(
        modifier = Modifier
            .screenContainer()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenBackButton(onClick = onNavigateBack)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.inflation_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.inflation_source, CpiSeedData.SOURCE, CpiSeedData.FETCHED),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (rows.isEmpty()) {
            Text(
                text = stringResource(Res.string.inflation_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(Res.string.inflation_change_caption),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            rows.forEach { row -> InflationRowItem(row) }
        }
    }
}

@Composable
private fun InflationRowItem(row: InflationRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("inflationRow-${row.currency.name}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = row.currency.symbol,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = row.currency.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(Res.string.inflation_through, row.latestMonth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = row.change12mPercent?.let(::formatPercentChange) ?: "–",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
