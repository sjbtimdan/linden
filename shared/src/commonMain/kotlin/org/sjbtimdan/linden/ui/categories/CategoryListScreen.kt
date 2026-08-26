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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ScreenMaxWidth
import org.sjbtimdan.linden.ui.ScreenPadding
import org.sjbtimdan.linden.ui.screenInsets
import org.sjbtimdan.linden.ui.theme.categoryAccent

private data class CategoryDialogState(
    val category: Category?,
    val name: String,
    val type: CategoryType,
)

@Composable
fun CategoryListScreen(viewModel: CategoryListViewModel, onNavigateBack: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
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
                    val accent = categoryAccent(category.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(role = Role.Button) {
                                dialogState = CategoryDialogState(
                                    category = category,
                                    name = category.name,
                                    type = category.type,
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
                                text = category.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                color = accent,
                            )
                        }
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
            isEditing = isEditing,
            onNameChange = { dialogState = state.copy(name = it) },
            onTypeChange = { dialogState = state.copy(type = it) },
            onSave = {
                val name = state.name.trim()
                if (name.isNotEmpty()) {
                    val existing = state.category
                    if (existing != null) {
                        viewModel.updateCategory(
                            existing.copy(name = name, type = state.type),
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
    CategoryType.Both -> "As Income/Expense"
}

private fun CategoryType.dialogLabel(): String = when (this) {
    CategoryType.Expense -> "Expense"
    CategoryType.Income -> "Income"
    CategoryType.Both -> "Both"
}
