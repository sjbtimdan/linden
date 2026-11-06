package org.sjbtimdan.linden.ui.budget

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import org.sjbtimdan.linden.model.Budget
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.budget_choose_category
import org.sjbtimdan.linden.resources.budget_delete
import org.sjbtimdan.linden.resources.budget_edit
import org.sjbtimdan.linden.resources.budget_empty
import org.sjbtimdan.linden.resources.budget_monthly_limit
import org.sjbtimdan.linden.resources.budget_new
import org.sjbtimdan.linden.resources.common_category
import org.sjbtimdan.linden.resources.common_invalid_amount
import org.sjbtimdan.linden.resources.common_save
import org.sjbtimdan.linden.resources.settings_nav_budgets
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.DialogCancelButton
import org.sjbtimdan.linden.ui.DialogDeleteButton
import org.sjbtimdan.linden.ui.ErrorSnackbar
import org.sjbtimdan.linden.ui.LocalBottomBarContentPadding
import org.sjbtimdan.linden.ui.ScreenHeader
import org.sjbtimdan.linden.ui.entry.AmountField
import org.sjbtimdan.linden.ui.entry.HIDDEN_AMOUNT
import org.sjbtimdan.linden.ui.entry.filterAmountInput
import org.sjbtimdan.linden.ui.entry.formatAmount
import org.sjbtimdan.linden.ui.entry.parseAmount
import org.sjbtimdan.linden.ui.pressFeedback
import org.sjbtimdan.linden.ui.rememberEntranceComplete
import org.sjbtimdan.linden.ui.screenContainer
import org.sjbtimdan.linden.ui.staggeredEntrance
import org.sjbtimdan.linden.ui.theme.DialogShape
import org.sjbtimdan.linden.ui.theme.accentColor
import org.sjbtimdan.linden.ui.theme.lindenCard

private data class BudgetDialogState(
    val budget: Budget?,
    val categoryName: String,
    val limitText: String,
    val limitError: String? = null,
)

@Composable
fun BudgetScreen(viewModel: BudgetViewModel, onNavigateBack: () -> Unit) {
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val hideTotal by viewModel.hideTotal.collectAsState()
    var dialogState by remember { mutableStateOf<BudgetDialogState?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = dialogState != null) {
        dialogState = null
    }

    viewModel.ErrorSnackbar(snackbarHostState)

    Column(
        modifier = Modifier.screenContainer(),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.settings_nav_budgets),
            onBack = onNavigateBack,
            actions = {
                IconButton(
                    onClick = {
                        dialogState = BudgetDialogState(null, "", "")
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(Res.string.budget_new),
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.budget_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            dialogState = BudgetDialogState(null, "", "")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(Res.string.budget_new))
                    }
                }
            }
        } else {
            val entranceComplete = rememberEntranceComplete(budgets.size)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = LocalBottomBarContentPadding.current),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(budgets, key = { _, budget -> budget.categoryName }) { index, budget ->
                    val accent = accentColor(budget.categoryName)
                    val interactionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(index, !entranceComplete)
                            .pressFeedback(interactionSource)
                            .lindenCard()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                                role = Role.Button,
                            ) {
                                dialogState = BudgetDialogState(
                                    budget = budget,
                                    categoryName = budget.categoryName,
                                    limitText = formatAmount(budget.limitMinor, defaultCurrency.decimalDigits),
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
                                text = budget.categoryName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                color = accent,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = budget.categoryName,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(Res.string.budget_monthly_limit),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = if (hideTotal) {
                                HIDDEN_AMOUNT
                            } else {
                                formatAmount(budget.limitMinor, defaultCurrency.decimalDigits)
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
        // Last child of the full-height column so the host anchors to the
        // bottom; as a sibling it would sit at the screen's top start and the
        // negative offset would push it off-screen.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.offset(y = -LocalBottomBarContentPadding.current),
        )
    }

    dialogState?.let { state ->
        val isEditing = state.budget != null
        val invalidLimitError = stringResource(Res.string.common_invalid_amount)
        BudgetDialog(
            categoryName = state.categoryName,
            limitText = state.limitText,
            limitError = state.limitError,
            categories = categories.filter { it.type != CategoryType.Income },
            isEditing = isEditing,
            onCategoryChange = { dialogState = state.copy(categoryName = it) },
            onLimitChange = { dialogState = state.copy(limitText = it, limitError = null) },
            onDelete = {
                val existing = state.budget
                if (existing != null) {
                    dialogState = null
                    viewModel.deleteBudget(existing.categoryName)
                }
            },
            onSave = {
                val limit = parseAmount(state.limitText, defaultCurrency.decimalDigits)
                if (limit == null || limit <= 0) {
                    dialogState = state.copy(limitError = invalidLimitError)
                } else if (viewModel.saveBudget(state.categoryName, limit)) {
                    dialogState = null
                }
            },
            onDismiss = { dialogState = null },
        )
    }
}

@Composable
private fun BudgetDialog(
    categoryName: String,
    limitText: String,
    limitError: String?,
    categories: List<Category>,
    isEditing: Boolean,
    onCategoryChange: (String) -> Unit,
    onLimitChange: (String) -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        title = {
            Text(if (isEditing) stringResource(Res.string.budget_edit) else stringResource(Res.string.budget_new))
        },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.common_category),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Box {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = onCategoryChange,
                        label = { Text(stringResource(Res.string.common_category)) },
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = stringResource(Res.string.budget_choose_category),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    onCategoryChange(category.name)
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                AmountField(
                    value = limitText,
                    label = stringResource(Res.string.budget_monthly_limit),
                    suffix = null,
                    warning = limitError,
                    onValueChange = { onLimitChange(filterAmountInput(it)) },
                )
                if (isEditing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    DialogDeleteButton(
                        label = stringResource(Res.string.budget_delete),
                        onDelete = onDelete,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = categoryName.isNotBlank(),
            ) {
                Text(stringResource(Res.string.common_save))
            }
        },
        dismissButton = { DialogCancelButton(onDismiss) },
    )
}
