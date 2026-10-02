package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_category
import org.sjbtimdan.linden.resources.ledger_adjust_balance
import org.sjbtimdan.linden.resources.ledger_adjust_bank_balance
import org.sjbtimdan.linden.resources.ledger_adjust_confirm
import org.sjbtimdan.linden.resources.ledger_adjust_current_balance
import org.sjbtimdan.linden.resources.ledger_adjust_explainer
import org.sjbtimdan.linden.resources.ledger_adjust_explainer_expense
import org.sjbtimdan.linden.resources.ledger_adjust_explainer_income
import org.sjbtimdan.linden.ui.DialogCancelButton
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.accounts.balanceAdjustment
import org.sjbtimdan.linden.ui.entry.AmountField
import org.sjbtimdan.linden.ui.entry.HIDDEN_AMOUNT
import org.sjbtimdan.linden.ui.entry.filterAmountInput
import org.sjbtimdan.linden.ui.entry.formatAmount
import org.sjbtimdan.linden.ui.entry.parseAmount
import org.sjbtimdan.linden.ui.theme.DialogShape

/** In-progress balance adjustment: the account, its current balance and the edited fields. */
internal data class AdjustBalanceDialogState(
    val account: AccountWithBalance,
    val currentBalance: Long,
    val targetBalanceText: String,
    val categoryQuery: String = "",
)

/**
 * Hosts the balance-adjustment dialog for [state]: loads the account's used
 * categories, orders them, resolves the category typed into the field and saves
 * the adjustment. Field edits are reported through [onStateChange] so the
 * in-progress text survives while the dialog is open.
 */
@Composable
internal fun AdjustBalanceHost(
    viewModel: LedgerViewModel,
    state: AdjustBalanceDialogState,
    hideAmounts: Boolean,
    onStateChange: (AdjustBalanceDialogState) -> Unit,
    onDismiss: () -> Unit,
) {
    val targetBalance = parseAmount(state.targetBalanceText, state.account.account.currency.decimalDigits)
    val allCategories by viewModel.categories.collectAsState()
    var usedCategories by remember(state.account.account.id) { mutableStateOf<List<Category>>(emptyList()) }
    LaunchedEffect(state.account.account.id) {
        usedCategories = viewModel.usedCategories(state.account.account.id)
    }
    val usedIds = usedCategories.map { it.id }.toSet()
    val orderedCategories = usedCategories +
        allCategories.filterNot { it.id in usedIds }.sortedBy { it.name }
    val query = state.categoryQuery.trim()
    val visibleCategories = if (query.isEmpty()) {
        orderedCategories
    } else {
        orderedCategories.filter { it.name.contains(query, ignoreCase = true) }
    }
    // The category is selected by an exact (case-insensitive) match on the text field.
    val selectedCategory = orderedCategories.firstOrNull { it.name.equals(query, ignoreCase = true) }
    AdjustBalanceDialog(
        account = state.account.account,
        currentBalance = state.currentBalance,
        hideAmounts = hideAmounts,
        targetBalanceText = state.targetBalanceText,
        categoryQuery = state.categoryQuery,
        categories = visibleCategories,
        selectedCategoryId = selectedCategory?.id,
        onCategoryQueryChange = { onStateChange(state.copy(categoryQuery = it)) },
        onCategorySelect = { id ->
            val name = orderedCategories.firstOrNull { it.id == id }?.name ?: return@AdjustBalanceDialog
            onStateChange(state.copy(categoryQuery = name))
        },
        onTargetBalanceChange = { onStateChange(state.copy(targetBalanceText = it)) },
        onSave = {
            if (targetBalance != null && selectedCategory != null) {
                viewModel.adjustBalance(state.account.account, targetBalance, selectedCategory)
                onDismiss()
            }
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun AdjustBalanceDialog(
    account: Account,
    currentBalance: Long,
    hideAmounts: Boolean,
    targetBalanceText: String,
    categoryQuery: String,
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategoryQueryChange: (String) -> Unit,
    onCategorySelect: (Long) -> Unit,
    onTargetBalanceChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val targetBalance = parseAmount(targetBalanceText, account.currency.decimalDigits)
    val adjustment = targetBalance?.let { balanceAdjustment(currentBalance, it) }
    val canSave = adjustment != null && !adjustment.isZero && selectedCategoryId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        title = { Text(stringResource(Res.string.ledger_adjust_balance)) },
        text = {
            Column {
                val explainer = when {
                    adjustment == null || adjustment.isZero ->
                        stringResource(Res.string.ledger_adjust_explainer)

                    adjustment.delta > 0 -> stringResource(Res.string.ledger_adjust_explainer_income)

                    else -> stringResource(Res.string.ledger_adjust_explainer_expense)
                }
                Text(
                    text = explainer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        Res.string.ledger_adjust_current_balance,
                        if (hideAmounts) {
                            HIDDEN_AMOUNT
                        } else {
                            formatAmount(currentBalance, account.currency.decimalDigits)
                        },
                        account.currency.symbol,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                AmountField(
                    value = targetBalanceText,
                    label = stringResource(Res.string.ledger_adjust_bank_balance),
                    suffix = account.currency.symbol,
                    warning = null,
                    onValueChange = { onTargetBalanceChange(filterAmountInput(it)) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = categoryQuery,
                    onValueChange = onCategoryQueryChange,
                    label = { Text(stringResource(Res.string.common_category)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = category.id == selectedCategoryId,
                            onClick = { onCategorySelect(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = canSave) {
                Text(stringResource(Res.string.ledger_adjust_confirm))
            }
        },
        dismissButton = { DialogCancelButton(onDismiss) },
    )
}
