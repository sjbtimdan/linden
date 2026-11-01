package org.sjbtimdan.linden.ui.ledger

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.accounts_empty_none
import org.sjbtimdan.linden.resources.ledger_accounts_all_hidden
import org.sjbtimdan.linden.resources.ledger_accounts_no_match
import org.sjbtimdan.linden.resources.ledger_action_create_account
import org.sjbtimdan.linden.resources.ledger_action_manage_accounts
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.entry.formatAmount

/** The accounts view: period-end balances with search, conversions and Adjust Balance. */
@Composable
internal fun LedgerAccountsView(
    accounts: List<Account>,
    balances: List<AccountWithBalance>,
    conversions: Map<Long, Long?>,
    defaultCurrency: Currency,
    hideAmounts: Boolean,
    searchQuery: String,
    canAdjustBalance: Boolean,
    currentBalances: Map<Long, Long>,
    onEmptyAction: () -> Unit,
    onAccountClick: (AccountWithBalance) -> Unit,
    onAdjustBalance: (AdjustBalanceDialogState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filter = searchQuery.trim()
    val shownBalances =
        if (filter.isEmpty()) {
            balances
        } else {
            balances.filter { it.account.name.contains(filter, ignoreCase = true) }
        }
    AccountsList(
        balances = shownBalances,
        conversions = conversions,
        defaultCurrency = defaultCurrency,
        hideAmounts = hideAmounts,
        emptyMessage = when {
            filter.isNotEmpty() && balances.isNotEmpty() ->
                stringResource(Res.string.ledger_accounts_no_match)

            // No visible accounts at all: distinguish a brand-new app from
            // an app whose every account is hidden.
            accounts.isEmpty() -> stringResource(Res.string.accounts_empty_none)

            else -> stringResource(Res.string.ledger_accounts_all_hidden)
        },
        emptyActionLabel = when {
            filter.isNotEmpty() && balances.isNotEmpty() -> null

            filter.isEmpty() && accounts.isEmpty() ->
                stringResource(Res.string.ledger_action_create_account)

            filter.isEmpty() -> stringResource(Res.string.ledger_action_manage_accounts)

            else -> null
        },
        onEmptyAction = onEmptyAction,
        canAdjustBalance = canAdjustBalance,
        onAccountClick = onAccountClick,
        onAdjustBalance = { item ->
            val current = currentBalances[item.account.id] ?: item.account.initialBalance
            onAdjustBalance(
                AdjustBalanceDialogState(
                    account = item,
                    currentBalance = current,
                    targetBalanceText = formatAmount(current, item.account.currency.decimalDigits),
                ),
            )
        },
        modifier = modifier,
    )
}
