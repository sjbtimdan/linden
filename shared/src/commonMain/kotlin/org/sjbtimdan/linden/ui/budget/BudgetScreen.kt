package org.sjbtimdan.linden.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Budget
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ScreenMaxWidth
import org.sjbtimdan.linden.ui.ScreenPadding
import org.sjbtimdan.linden.ui.entry.formatAmount
import org.sjbtimdan.linden.ui.entry.parseAmount
import org.sjbtimdan.linden.ui.screenInsets
import org.sjbtimdan.linden.ui.theme.CardShape
import org.sjbtimdan.linden.ui.theme.DialogShape
import org.sjbtimdan.linden.ui.theme.accentColor

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
    var dialogState by remember { mutableStateOf<BudgetDialogState?>(null) }

    BackHandler(enabled = dialogState != null) {
        dialogState = null
    }

    Column(
        modifier = Modifier
            .screenInsets()
            .fillMaxSize()
            .padding(ScreenPadding)
            .widthIn(max = ScreenMaxWidth),
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
            Text("+ New Budget")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No budgets yet. Add one to track a category's monthly spending.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(budgets, key = { it.categoryName }) { budget ->
                    val accent = accentColor(budget.categoryName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(role = Role.Button) {
                                dialogState = BudgetDialogState(
                                    budget = budget,
                                    categoryName = budget.categoryName,
                                    limitText = formatAmount(budget.limitMinor),
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
                                text = "Monthly limit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = formatAmount(budget.limitMinor),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }

    dialogState?.let { state ->
        val isEditing = state.budget != null
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
                val limit = parseAmount(state.limitText)
                if (limit == null || limit <= 0) {
                    dialogState = state.copy(limitError = "Enter a valid amount")
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
            Text(if (isEditing) "Edit Budget" else "New Budget")
        },
        text = {
            Column {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Box {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = onCategoryChange,
                        label = { Text("Category") },
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Choose category",
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
                OutlinedTextField(
                    value = limitText,
                    onValueChange = onLimitChange,
                    label = { Text("Monthly limit") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = limitError != null,
                    supportingText = limitError?.let { error -> { Text(error) } },
                    trailingIcon = if (limitText.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { onLimitChange("") },
                            ) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isEditing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Budget")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = categoryName.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
