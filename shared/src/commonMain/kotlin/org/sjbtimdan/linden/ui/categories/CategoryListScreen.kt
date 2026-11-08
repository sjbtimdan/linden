package org.sjbtimdan.linden.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.categories_empty_no_match
import org.sjbtimdan.linden.resources.categories_empty_none
import org.sjbtimdan.linden.resources.categories_new
import org.sjbtimdan.linden.resources.common_categories
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ErrorSnackbar
import org.sjbtimdan.linden.ui.LocalBottomBarContentPadding
import org.sjbtimdan.linden.ui.ScreenHeader
import org.sjbtimdan.linden.ui.SearchField
import org.sjbtimdan.linden.ui.rememberEntranceComplete
import org.sjbtimdan.linden.ui.screenContainer
import org.sjbtimdan.linden.ui.staggeredEntrance
import org.sjbtimdan.linden.ui.theme.accentColor
import org.sjbtimdan.linden.ui.theme.lindenCard

@Composable
fun CategoryListScreen(viewModel: CategoryListViewModel, onNavigateBack: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoriesWithEntries by viewModel.categoriesWithEntries.collectAsState()
    var dialogState by remember { mutableStateOf<CategoryDialogState?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = dialogState != null) {
        dialogState = null
    }

    viewModel.ErrorSnackbar(snackbarHostState)

    Column(
        modifier = Modifier.screenContainer(),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.common_categories),
            onBack = onNavigateBack,
            actions = {
                IconButton(
                    onClick = {
                        dialogState = CategoryDialogState(null, "", CategoryType.Expense)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(Res.string.categories_new),
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SearchField(value = searchQuery, onValueChange = viewModel::setSearchQuery)

        Spacer(modifier = Modifier.height(16.dp))

        if (categories.isEmpty()) {
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
                        text = if (searchQuery.isBlank()) {
                            stringResource(Res.string.categories_empty_none)
                        } else {
                            stringResource(Res.string.categories_empty_no_match)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (searchQuery.isBlank()) {
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
                    }
                }
            }
        } else {
            val entranceComplete = rememberEntranceComplete(categories.size)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = LocalBottomBarContentPadding.current),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(categories, key = { _, category -> category.id }, contentType = { _, _ -> "category" }) { index, category ->
                    val accent = accentColor(category.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(index, !entranceComplete)
                            .lindenCard()
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
        // Last child of the full-height column so the host anchors to the
        // bottom; as a sibling it would sit at the screen's top start and the
        // negative offset would push it off-screen.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.offset(y = -LocalBottomBarContentPadding.current),
        )
    }

    CategoryDialogHost(
        state = dialogState,
        isEditing = dialogState?.category != null,
        canDelete = dialogState?.category?.let { it.id !in categoriesWithEntries } == true,
        onStateChange = { dialogState = it },
        onSave = { name, type, icon ->
            val existing = dialogState?.category
            if (existing != null) {
                viewModel.updateCategory(existing.copy(name = name, type = type, icon = icon))
            } else {
                viewModel.createCategory(name, type, icon)
            }
        },
        onDelete = { viewModel.deleteCategory(it.id) },
    )
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
