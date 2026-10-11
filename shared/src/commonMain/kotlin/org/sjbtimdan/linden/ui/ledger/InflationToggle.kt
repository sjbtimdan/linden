package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.ledger_inflation_note
import org.sjbtimdan.linden.resources.ledger_inflation_toggle

/**
 * The ledger's "In today's money" switch: a chip that restates displayed
 * amounts in the latest stored month's CPI, with the covered month as a
 * caption while it is on.
 */
@Composable
internal fun InflationToggle(
    checked: Boolean,
    throughMonth: String?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = checked,
            onClick = onToggle,
            label = { Text(stringResource(Res.string.ledger_inflation_toggle)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Percent,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            modifier = Modifier.testTag("inflationToggle"),
        )
        if (checked && throughMonth != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.ledger_inflation_note, throughMonth),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
