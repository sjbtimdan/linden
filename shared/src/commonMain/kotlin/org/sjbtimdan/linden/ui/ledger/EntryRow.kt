package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.TransferEntry

private fun Entry.title(): String {
    val description = description?.takeIf { it.isNotBlank() }
    return when (this) {
        is TransferEntry -> description ?: "Transfer"
        else -> description ?: requireNotNull(category).name
    }
}

private fun Entry.subtitle(): String = when (this) {
    is TransferEntry -> "${account.name} → ${toAccount.name}"
    else -> account.name
}

private fun Entry.amountLabel(): String = when (type) {
    EntryType.Expense -> "− ${formatAmount(amount)}"
    EntryType.Income -> "+ ${formatAmount(amount)}"
    EntryType.Transfer -> formatAmount(amount)
}

@Composable
private fun Entry.amountColor(): Color = when (type) {
    EntryType.Expense -> MaterialTheme.colorScheme.error
    EntryType.Income -> Color(0xFF43A047)
    EntryType.Transfer -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun EntryRow(
    entry: Entry,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title(),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = entry.subtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDateTime(entry.createdAt, entry.createdZone),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = entry.amountLabel(),
            style = MaterialTheme.typography.bodyLarge,
            color = entry.amountColor(),
        )
    }
    HorizontalDivider()
}