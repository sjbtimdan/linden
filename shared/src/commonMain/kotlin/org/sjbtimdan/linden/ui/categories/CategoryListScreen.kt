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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.categories_duplicate_name
import org.sjbtimdan.linden.resources.categories_empty_no_match
import org.sjbtimdan.linden.resources.categories_empty_none
import org.sjbtimdan.linden.resources.categories_new
import org.sjbtimdan.linden.resources.common_back
import org.sjbtimdan.linden.resources.common_clear
import org.sjbtimdan.linden.resources.common_search
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ScreenMaxWidth
import org.sjbtimdan.linden.ui.ScreenPadding
import org.sjbtimdan.linden.ui.screenInsets
import org.sjbtimdan.linden.ui.theme.CardShape
import org.sjbtimdan.linden.ui.theme.accentColor

@Composable
fun CategoryListScreen(viewModel: CategoryListViewModel, onNavigateBack: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoriesWithEntries by viewModel.categoriesWithEntries.collectAsState()
    var dialogState by remember { mutableStateOf<CategoryDialogState?>(null) }
    val duplicateNameError = stringResource(Res.string.categories_duplicate_name)

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
                contentDescription = stringResource(Res.string.common_back),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::setSearchQuery,
            label = { Text(stringResource(Res.string.common_search)) },
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
                    ) { Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.common_clear)) }
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
            Text(stringResource(Res.string.categories_new))
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
                    text = if (searchQuery.isBlank()) {
                        stringResource(Res.string.categories_empty_none)
                    } else {
                        stringResource(Res.string.categories_empty_no_match)
                    },
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
                        dialogState = state.copy(nameError = duplicateNameError)
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
                contentDescription = icon.a11yLabel(),
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
