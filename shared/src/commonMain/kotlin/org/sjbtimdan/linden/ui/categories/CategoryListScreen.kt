package org.sjbtimdan.linden.ui.categories

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
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.ui.entry.formatAmount
import org.sjbtimdan.linden.ui.theme.categoryColor

private data class CategoryDialogState(
    val category: Category?,
    val name: String,
    val type: CategoryType,
)

@Composable
fun CategoryListScreen(
    viewModel: CategoryListViewModel,
    onNavigateBack: () -> Unit,
) {
    val categories by viewModel.categories.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val totalMinor by viewModel.totalMinor.collectAsState()
    var dialogState by remember { mutableStateOf<CategoryDialogState?>(null) }

    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .padding(16.dp)
            .widthIn(max = 480.dp)
    ) {
        TextButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("< Settings")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (categories.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = totalMinor?.let { "${formatAmount(it)} ${defaultCurrency.symbol}" } ?: "–",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        FilledTonalButton(
            onClick = {
                dialogState = CategoryDialogState(null, "", CategoryType.Expense)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories, key = { it.category.id }) { item ->
                    val accent = categoryColor(item.category.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(role = Role.Button) {
                                dialogState = CategoryDialogState(
                                    category = item.category,
                                    name = item.category.name,
                                    type = item.category.type,
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = item.category.name.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = accent,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = item.category.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = item.category.type.displayName(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = item.balance?.let { "${formatAmount(it)} ${defaultCurrency.symbol}" } ?: "–",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    dialogState?.let { state ->
        val isEditing = state.category != null
        CategoryDialog(
            name = state.name,
            type = state.type,
            isEditing = isEditing,
            onNameChange = { dialogState = state.copy(name = it) },
            onTypeChange = { dialogState = state.copy(type = it) },
            onSave = {
                val name = state.name.trim()
                if (name.isNotEmpty()) {
                    val existing = state.category
                    if (existing != null) {
                        viewModel.updateCategory(
                            existing.copy(name = name, type = state.type)
                        )
                    } else {
                        viewModel.createCategory(name, state.type)
                    }
                    dialogState = null
                }
            },
            onDismiss = { dialogState = null },
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
