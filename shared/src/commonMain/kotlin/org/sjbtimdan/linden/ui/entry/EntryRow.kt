package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.theme.lindenColors

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
    EntryType.Expense -> "− ${formatAmountCompact(amount)} ${account.currency.symbol}"
    EntryType.Income -> "+ ${formatAmountCompact(amount)} ${account.currency.symbol}"
    EntryType.Transfer -> "${formatAmountCompact(amount)} ${account.currency.symbol}"
}

@Composable
private fun Entry.tintColor(): Color = when (type) {
    EntryType.Expense -> lindenColors().expense
    EntryType.Income -> lindenColors().income
    EntryType.Transfer -> lindenColors().transfer
}

@Composable
fun EntryRow(entry: Entry, onClick: () -> Unit) {
    val tint = entry.tintColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.type.icon(),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
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
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = entry.amountLabel(),
            style = MaterialTheme.typography.titleMedium,
            color = tint,
            maxLines = 1,
        )
    }
}
