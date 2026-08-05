package org.sjbtimdan.linden.ui.categories

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
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType

@Composable
fun CategoryListScreen(
    viewModel: CategoryListViewModel,
    onNavigateBack: () -> Unit,
) {
    val categories by viewModel.categories.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var dialogName by remember { mutableStateOf("") }
    var dialogType by remember { mutableStateOf(CategoryType.Expense) }

    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .padding(16.dp)
            .widthIn(max = 480.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Categories",
                fontSize = 28.sp,
            )
            TextButton(onClick = onNavigateBack) {
                Text("< Settings")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        FilledTonalButton(
            onClick = {
                editingCategory = null
                dialogName = ""
                dialogType = CategoryType.Expense
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("+ New Category")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No categories yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(categories, key = { it.id }) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                editingCategory = category
                                dialogName = category.name
                                dialogType = category.type
                                showDialog = true
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = category.type.displayName(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = ">",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (showDialog) {
        CategoryDialog(
            name = dialogName,
            type = dialogType,
            isEditing = editingCategory != null,
            onNameChange = { dialogName = it },
            onTypeChange = { dialogType = it },
            onSave = {
                val name = dialogName.trim()
                if (name.isNotEmpty()) {
                    val existing = editingCategory
                    if (existing != null) {
                        viewModel.updateCategory(
                            existing.copy(name = name, type = dialogType)
                        )
                    } else {
                        viewModel.createCategory(name, dialogType)
                    }
                    showDialog = false
                }
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun CategoryDialog(
    name: String,
    type: CategoryType,
    isEditing: Boolean,
    onNameChange: (String) -> Unit,
    onTypeChange: (CategoryType) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Edit Category" else "New Category")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CategoryType.entries.forEachIndexed { index, ct ->
                        SegmentedButton(
                            selected = type == ct,
                            onClick = { onTypeChange(ct) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = CategoryType.entries.size,
                            ),
                        ) {
                            Text(ct.displayName())
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
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

private fun CategoryType.displayName(): String = when (this) {
    CategoryType.Expense -> "Expense"
    CategoryType.Income -> "Income"
    CategoryType.Both -> "Both"
}
