package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.entry.formatAmount
import org.sjbtimdan.linden.ui.theme.accentColor

/**
 * Category totals at the end of the selected period, or the empty state.
 * [emptyActionLabel]/[onEmptyAction] turn the empty state into a guided one:
 * a button pointing at the next step for a brand-new user.
 */
@Composable
fun CategoryTotalsList(
    categories: List<CategoryWithTotal>,
    currency: Currency,
    modifier: Modifier,
    emptyMessage: String,
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    onCategoryClick: (CategoryWithTotal) -> Unit,
) {
    if (categories.isEmpty()) {
        EmptyState(
            message = emptyMessage,
            modifier = modifier,
            actionLabel = emptyActionLabel,
            onAction = onEmptyAction,
        )
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories, key = { it.category?.id ?: 0L }) { item ->
                val categoryName = item.category?.name ?: "Uncategorized"
                val accent = item.category?.let { accentColor(it.name) }
                    ?: MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(role = Role.Button) { onCategoryClick(item) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            val icon = item.category?.icon?.imageVector()
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Text(
                                    text = categoryName.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = accent,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "${item.count} ${if (item.count == 1) "entry" else "entries"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = formatTotal(item.total, currency),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    item.budget?.let { budget ->
                        Spacer(modifier = Modifier.height(8.dp))
                        BudgetProgressBar(
                            spent = item.total,
                            limit = budget,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressBar(spent: Long, limit: Long, modifier: Modifier = Modifier) {
    val absoluteSpent = if (spent < 0) -spent else spent
    val fraction = if (limit <= 0) 0f else (absoluteSpent.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
    val overBudget = limit > 0 && absoluteSpent > limit
    val color = when {
        overBudget -> MaterialTheme.colorScheme.error
        fraction >= 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { fraction },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${formatAmount(absoluteSpent)} of ${formatAmount(limit)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
