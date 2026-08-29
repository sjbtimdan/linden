package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.ui.entry.parseAmount

/**
 * Chip that opens a small popover for filtering entries by amount. The chip shows
 * "Amount: All" when no filter is set, or the active filter (e.g. "> 50", "< 100",
 * "~ 20") once one is applied. The popover lets the user pick an operator by tapping
 * (no keyboard needed for `>`, `<`, `~`) and type a value.
 */
@Composable
fun AmountFilterChip(
    filter: AmountFilter?,
    onApply: (AmountFilter) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var operator by remember { mutableStateOf(filter?.operator ?: AmountOperator.GreaterThan) }
    var amountText by remember { mutableStateOf(filter?.let { formatFilterValue(it) } ?: "") }

    // Keep the draft in sync when the applied filter changes from outside.
    LaunchedEffect(filter) {
        operator = filter?.operator ?: AmountOperator.GreaterThan
        amountText = filter?.let { formatFilterValue(it) } ?: ""
    }

    Box {
        FilterChip(
            selected = filter != null,
            onClick = { expanded = true },
            modifier = modifier.testTag("amountFilterChip"),
            label = {
                Text(
                    text = filter?.let { formatFilter(it) } ?: "Amount: All",
                    style = MaterialTheme.typography.labelMedium,
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Filter by amount",
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AmountOperator.entries.forEach { op ->
                        FilterChip(
                            selected = operator == op,
                            onClick = { operator = op },
                            label = { Text(op.symbol()) },
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("amountFilterValue"),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (filter != null) {
                        TextButton(onClick = {
                            onClear()
                            expanded = false
                        }) {
                            Text("Clear")
                        }
                    }
                    Button(
                        onClick = {
                            parseAmount(amountText)?.let { minor ->
                                onApply(AmountFilter(operator, minor))
                                expanded = false
                            }
                        },
                        enabled = parseAmount(amountText) != null,
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

private fun AmountOperator.symbol(): String = when (this) {
    AmountOperator.GreaterThan -> ">"
    AmountOperator.LessThan -> "<"
    AmountOperator.Approximately -> "~"
}

private fun formatFilter(filter: AmountFilter): String = "${filter.operator.symbol()} ${formatFilterValue(filter)}"

private fun formatFilterValue(filter: AmountFilter): String =
    (filter.minor / 100).toString() + if (filter.minor % 100 != 0L) {
        "." + (filter.minor % 100).toString().padStart(2, '0')
    } else {
        ""
    }
