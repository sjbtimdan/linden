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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ScreenMaxWidth
import org.sjbtimdan.linden.ui.ScreenPadding
import org.sjbtimdan.linden.ui.screenInsets
import org.sjbtimdan.linden.ui.theme.CardShape
import org.sjbtimdan.linden.ui.theme.DialogShape
import org.sjbtimdan.linden.ui.theme.accentColor

private data class CategoryDialogState(
    val category: Category?,
    val name: String,
    val type: CategoryType,
    val icon: CategoryIcon? = null,
    val nameError: String? = null,
)

@Composable
fun CategoryListScreen(viewModel: CategoryListViewModel, onNavigateBack: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoriesWithEntries by viewModel.categoriesWithEntries.collectAsState()
    var dialogState by remember { mutableStateOf<CategoryDialogState?>(null) }

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

        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::setSearchQuery,
            label = { Text("Search") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { viewModel.setSearchQuery("") },
                    ) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
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
                    text = if (searchQuery.isBlank()) "No categories yet." else "No matching categories.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    val accent = accentColor(category.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(role = Role.Button) {
                                dialogState = CategoryDialogState(
                                    category = category,
                                    name = category.name,
                                    type = category.type,
                                    icon = category.icon,
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryIconBox(category.icon, category.name, accent)
                        Spacer(modifier = Modifier.width(12.dp))
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
                        Spacer(modifier = Modifier.weight(1f))
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
            icon = state.icon,
            nameError = state.nameError,
            isEditing = isEditing,
            canDelete = isEditing && state.category.id !in categoriesWithEntries,
            onNameChange = { dialogState = state.copy(name = it, nameError = null) },
            onTypeChange = { dialogState = state.copy(type = it) },
            onIconChange = { dialogState = state.copy(icon = it) },
            onDelete = {
                val existing = state.category
                if (existing != null) {
                    dialogState = null
                    viewModel.deleteCategory(existing.id)
                }
            },
            onSave = {
                val name = state.name.trim()
                if (name.isNotEmpty()) {
                    val existing = state.category
                    val saved = if (existing != null) {
                        viewModel.updateCategory(
                            existing.copy(name = name, type = state.type, icon = state.icon),
                        )
                    } else {
                        viewModel.createCategory(name, state.type, state.icon)
                    }
                    if (saved) {
                        dialogState = null
                    } else {
                        dialogState = state.copy(nameError = "A category with this name already exists")
                    }
                }
            },
            onDismiss = { dialogState = null },
        )
    }
}

@Composable
private fun CategoryIconBox(icon: CategoryIcon?, categoryName: String, accent: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon.imageVector(),
                contentDescription = icon.label,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Text(
                text = categoryName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
        }
    }
}

@Composable
private fun CategoryDialog(
    name: String,
    type: CategoryType,
    icon: CategoryIcon?,
    nameError: String?,
    isEditing: Boolean,
    canDelete: Boolean,
    onNameChange: (String) -> Unit,
    onTypeChange: (CategoryType) -> Unit,
    onIconChange: (CategoryIcon?) -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
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
                    isError = nameError != null,
                    supportingText = nameError?.let { error -> { Text(error) } },
                    trailingIcon = if (name.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { onNameChange("") },
                            ) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                IconPicker(
                    selected = icon,
                    onSelect = onIconChange,
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
                            Text(ct.dialogLabel())
                        }
                    }
                }
                if (isEditing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!canDelete) {
                        Text(
                            text = "This category cannot be deleted: it has entries.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        enabled = canDelete,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Category")
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

@Composable
private fun IconPicker(selected: CategoryIcon?, onSelect: (CategoryIcon?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryIcon.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { iconOption ->
                    val isSelected = iconOption == selected
                    val accent = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                            )
                            .clickable(
                                role = Role.Button,
                                onClickLabel = iconOption.label,
                            ) {
                                onSelect(if (isSelected) null else iconOption)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = iconOption.imageVector(),
                            contentDescription = iconOption.label,
                            tint = accent,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                // Fill remaining space in incomplete rows
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

private fun CategoryType.displayName(): String = when (this) {
    CategoryType.Expense -> "Expense"
    CategoryType.Income -> "Income"
    CategoryType.Both -> "As Income/Expense"
}

private fun CategoryType.dialogLabel(): String = when (this) {
    CategoryType.Expense -> "Expense"
    CategoryType.Income -> "Income"
    CategoryType.Both -> "Both"
}
