package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.entry.formatAmount
import org.sjbtimdan.linden.ui.theme.accentColor
import org.sjbtimdan.linden.ui.theme.lindenColors
import kotlin.math.abs

/**
 * Compact month-to-date spending summary: total spent this month vs the same
 * day-range of last month, plus the top expense categories with their share.
 *
 * When [onToggleCollapsed] is provided the card can be collapsed to a slim
 * header row (a chevron expands it again), so the month summary never takes
 * more space than the user wants.
 */
@Composable
fun SpendingInsightsCard(
    insights: SpendingInsights,
    currency: Currency,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
    onToggleCollapsed: (() -> Unit)? = null,
) {
    val colors = lindenColors()
    val titleColor = MaterialTheme.colorScheme.onSurfaceVariant
    if (collapsed && onToggleCollapsed != null) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = modifier.testTag("spendingInsightsHeader"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleCollapsed)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Spending insights",
                    style = MaterialTheme.typography.labelLarge,
                    color = titleColor,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Expand insights",
                    tint = titleColor,
                )
            }
        }
        return
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.testTag("spendingInsightsCard"),
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Spending insights",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (onToggleCollapsed != null) {
                    IconButton(
                        onClick = onToggleCollapsed,
                        modifier = Modifier.testTag("collapseInsights"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExpandLess,
                            contentDescription = "Collapse insights",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Spent this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatAmount(insights.currentSpent),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currency.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            insights.changePercent?.let { change ->
                Spacer(modifier = Modifier.height(4.dp))
                val increased = change > 0
                val tint = if (increased) colors.expense else colors.income
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (increased) {
                            Icons.AutoMirrored.Filled.TrendingUp
                        } else {
                            Icons.AutoMirrored.Filled.TrendingDown
                        },
                        contentDescription = null,
                        tint = tint,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${abs(change)}% vs last month",
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                    )
                }
            }
            if (insights.topCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                insights.topCategories.forEach { share ->
                    CategoryShareRow(share = share)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryShareRow(share: CategoryShare) {
    val name = share.category?.name ?: "Uncategorized"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${share.sharePercent}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(modifier = Modifier.height(2.dp))
    LinearProgressIndicator(
        progress = { share.sharePercent / 100f },
        color = accentColor(name),
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
    )
}
