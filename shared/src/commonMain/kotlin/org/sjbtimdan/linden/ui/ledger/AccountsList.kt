package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.ledger_adjust_balance
import org.sjbtimdan.linden.resources.ledger_adjust_latest_period_only
import org.sjbtimdan.linden.resources.ledger_more_options
import org.sjbtimdan.linden.resources.ledger_view_entries
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.entry.HIDDEN_AMOUNT
import org.sjbtimdan.linden.ui.entry.formatAmountCompact
import org.sjbtimdan.linden.ui.theme.CardElevation
import org.sjbtimdan.linden.ui.theme.CardShape

/**
 * The account balances at the end of the selected period, or the empty state.
 * Tapping a row drills into its entries ([onAccountClick]); the overflow menu
 * offers the same drill-in plus Adjust Balance, which is disabled with a
 * reason while [canAdjustBalance] is false so the action never pretends to
 * work. [emptyActionLabel]/[onEmptyAction] turn the empty state into a guided
 * one: a button pointing at the next step for a brand-new user. [hideAmounts]
 * masks each balance while "Hide Totals" is on.
 */
@Composable
fun AccountsList(
    balances: List<AccountWithBalance>,
    modifier: Modifier = Modifier,
    hideAmounts: Boolean = false,
    emptyMessage: String,
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    canAdjustBalance: Boolean = true,
    onAccountClick: (AccountWithBalance) -> Unit,
    onAdjustBalance: (AccountWithBalance) -> Unit,
) {
    if (balances.isEmpty()) {
        EmptyState(
            message = emptyMessage,
            modifier = modifier,
            actionLabel = emptyActionLabel,
            onAction = onEmptyAction,
        )
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(balances, key = { it.account.id }) { item ->
                val account = item.account
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(CardElevation, CardShape, clip = false)
                        .clip(CardShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
                        .clickable(role = Role.Button) { onAccountClick(item) }
                        .padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
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
                        text = if (hideAmounts) {
                            HIDDEN_AMOUNT
                        } else {
                            "${formatAmountCompact(item.balance)} ${account.currency.symbol}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(Res.string.ledger_more_options),
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.ledger_view_entries)) },
                                onClick = {
                                    menuOpen = false
                                    onAccountClick(item)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.ledger_adjust_balance)) },
                                enabled = canAdjustBalance,
                                onClick = {
                                    menuOpen = false
                                    onAdjustBalance(item)
                                },
                            )
                            if (!canAdjustBalance) {
                                Text(
                                    text = stringResource(Res.string.ledger_adjust_latest_period_only),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
