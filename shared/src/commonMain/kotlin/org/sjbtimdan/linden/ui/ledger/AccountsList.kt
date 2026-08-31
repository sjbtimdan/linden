package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.entry.formatAmountCompact

/** The account balances at the end of the selected period, or the empty state. */
@Composable
fun AccountsList(
    balances: List<AccountWithBalance>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No accounts yet.",
    canAdjustBalance: Boolean = true,
    onAdjustBalance: (AccountWithBalance) -> Unit,
    onAdjustBalanceUnavailable: (AccountWithBalance) -> Unit = {},
) {
    if (balances.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(balances, key = { it.account.id }) { item ->
                val account = item.account
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = account.currency.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "${formatAmountCompact(item.balance)} ${account.currency.symbol}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Adjust Balance") },
                                onClick = {
                                    menuOpen = false
                                    if (canAdjustBalance) {
                                        onAdjustBalance(item)
                                    } else {
                                        onAdjustBalanceUnavailable(item)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
