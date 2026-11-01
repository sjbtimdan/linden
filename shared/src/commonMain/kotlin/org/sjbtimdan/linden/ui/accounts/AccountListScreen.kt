package org.sjbtimdan.linden.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.accounts_empty_no_match
import org.sjbtimdan.linden.resources.accounts_empty_none
import org.sjbtimdan.linden.resources.accounts_hidden
import org.sjbtimdan.linden.resources.accounts_hide_body
import org.sjbtimdan.linden.resources.accounts_hide_confirm
import org.sjbtimdan.linden.resources.accounts_hide_title
import org.sjbtimdan.linden.resources.accounts_initial_balance
import org.sjbtimdan.linden.resources.accounts_new
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ConfirmDialog
import org.sjbtimdan.linden.ui.ErrorSnackbar
import org.sjbtimdan.linden.ui.LocalBottomBarContentPadding
import org.sjbtimdan.linden.ui.ScreenBackButton
import org.sjbtimdan.linden.ui.SearchField
import org.sjbtimdan.linden.ui.entry.HIDDEN_AMOUNT
import org.sjbtimdan.linden.ui.entry.VisibilityOffIcon
import org.sjbtimdan.linden.ui.entry.formatAmount
import org.sjbtimdan.linden.ui.entry.formatAmountCompact
import org.sjbtimdan.linden.ui.rememberEntranceComplete
import org.sjbtimdan.linden.ui.screenContainer
import org.sjbtimdan.linden.ui.staggeredEntrance
import org.sjbtimdan.linden.ui.theme.accentColor
import org.sjbtimdan.linden.ui.theme.lindenCard

@Composable
fun AccountListScreen(viewModel: AccountListViewModel, onNavigateBack: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val accountsWithEntries by viewModel.accountsWithEntries.collectAsState()
    val allTimeBalances by viewModel.allTimeBalances.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val hideTotal by viewModel.hideTotal.collectAsState()
    var dialogState by remember { mutableStateOf<AccountDialogState?>(null) }
    // Set when hiding an account with a non-zero all-time balance: the confirm
    // dialog must be answered before the account is actually hidden.
    var hideConfirmation by remember { mutableStateOf<AccountDialogState?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = dialogState != null) {
        dialogState = null
    }

    viewModel.ErrorSnackbar(snackbarHostState)

    Column(
        modifier = Modifier.screenContainer(),
    ) {
        ScreenBackButton(onClick = onNavigateBack)

        Spacer(modifier = Modifier.height(16.dp))

        SearchField(value = searchQuery, onValueChange = viewModel::setSearchQuery)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                dialogState = AccountDialogState(null, "", defaultCurrency, "")
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(Res.string.accounts_new))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isBlank()) {
                        stringResource(Res.string.accounts_empty_none)
                    } else {
                        stringResource(Res.string.accounts_empty_no_match)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val entranceComplete = rememberEntranceComplete(accounts.size)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = LocalBottomBarContentPadding.current),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(accounts, key = { _, account -> account.id }) { index, account ->
                    val accent = accentColor(account.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(index, !entranceComplete)
                            .lindenCard()
                            .clickable(role = Role.Button) {
                                dialogState = AccountDialogState(
                                    account = account,
                                    name = account.name,
                                    currency = account.currency,
                                    initialBalanceText = formatAmount(
                                        account.initialBalance,
                                        account.currency.decimalDigits,
                                    ),
                                    hidden = account.hidden,
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = account.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                color = accent,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = account.currency.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (account.hidden) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = VisibilityOffIcon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = stringResource(Res.string.accounts_hidden),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val balanceLabel = "${formatAmountCompact(
                                account.initialBalance,
                                account.currency.decimalDigits,
                            )} ${account.currency.symbol}"
                            Text(
                                text = if (hideTotal) HIDDEN_AMOUNT else balanceLabel,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(Res.string.accounts_initial_balance),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.offset(y = -LocalBottomBarContentPadding.current),
    )

    AccountDialogHost(
        state = dialogState,
        isEditing = dialogState?.account != null,
        canChangeCurrency = dialogState?.account?.let { it.id !in accountsWithEntries } ?: true,
        canDelete = dialogState?.account?.let { it.id !in accountsWithEntries } == true,
        onStateChange = { dialogState = it },
        onSave = { name, currency, initialBalance, _ ->
            val existing = dialogState?.account
            if (existing != null) {
                viewModel.updateAccount(
                    existing.copy(
                        name = name,
                        currency = currency,
                        initialBalance = initialBalance,
                    ),
                )
            } else {
                viewModel.createAccount(name, currency, initialBalance)
            }
        },
        onHiddenChange = { current, hidden ->
            val existing = current.account
            if (existing != null) {
                if (hidden && (allTimeBalances[existing.id] ?: existing.initialBalance) != 0L) {
                    // A non-zero account is about to leave the totals: ask first.
                    hideConfirmation = current
                } else {
                    viewModel.setHidden(existing.id, hidden)
                    dialogState = current.copy(hidden = hidden)
                }
            }
        },
        onDelete = { viewModel.deleteAccount(it.id) },
    )

    hideConfirmation?.let { pending ->
        val account = pending.account
        val balance = account?.let { allTimeBalances[it.id] ?: it.initialBalance } ?: 0L
        ConfirmDialog(
            title = stringResource(Res.string.accounts_hide_title),
            body = stringResource(
                Res.string.accounts_hide_body,
                account?.name.orEmpty(),
                if (hideTotal) HIDDEN_AMOUNT else formatAmount(balance, account?.currency?.decimalDigits ?: 2),
                account?.currency?.symbol.orEmpty(),
            ),
            confirmLabel = stringResource(Res.string.accounts_hide_confirm),
            onConfirm = {
                val id = account?.id
                if (id != null) {
                    viewModel.setHidden(id, true)
                    dialogState = pending.copy(hidden = true)
                }
                hideConfirmation = null
            },
            onDismiss = { hideConfirmation = null },
        )
    }
}
