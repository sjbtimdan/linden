package org.sjbtimdan.linden.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.insights_expenses
import org.sjbtimdan.linden.resources.insights_income
import org.sjbtimdan.linden.resources.insights_vs_previous
import org.sjbtimdan.linden.ui.entry.DateLanguage
import org.sjbtimdan.linden.ui.ledger.BudgetProgressBar
import org.sjbtimdan.linden.ui.ledger.formatTotal
import org.sjbtimdan.linden.ui.theme.accentColor

/**
 * The selected month's category rows: expenses and incomes in their own
 * sections, each ranked by amount. A row shows the category icon in its
 * accent color, the amount, and either the budget progress (expense rows
 * with a budget) or the change against the previous month. Sections without
 * rows are omitted entirely.
 */
@Composable
internal fun CategoryBreakdownList(
    breakdown: MonthBreakdown,
    currency: Currency,
    language: DateLanguage,
    previousMonthLabel: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (breakdown.expenses.isNotEmpty()) {
            BreakdownSectionHeader(stringResource(Res.string.insights_expenses))
            breakdown.expenses.forEach { row ->
                BreakdownCard(
                    row = row,
                    currency = currency,
                    language = language,
                    previousMonthLabel = previousMonthLabel,
                )
            }
        }
        if (breakdown.incomes.isNotEmpty()) {
            BreakdownSectionHeader(stringResource(Res.string.insights_income))
            breakdown.incomes.forEach { row ->
                BreakdownCard(
                    row = row,
                    currency = currency,
                    language = language,
                    previousMonthLabel = previousMonthLabel,
                )
            }
        }
    }
}

@Composable
private fun BreakdownSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun BreakdownCard(
    row: CategoryBreakdownRow,
    currency: Currency,
    language: DateLanguage,
    previousMonthLabel: String?,
) {
    val accent = accentColor(row.category.name)
    val amount = row.amountMinor
    val delta = if (amount != null && row.previousMinor != null) {
        previousMonthLabel?.let { label -> label to (amount - row.previousMinor) }
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                val icon = row.category.icon?.imageVector()
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = row.category.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall,
                        color = accent,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = row.category.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (amount != null) {
                Text(
                    text = amountLabel(amount, currency),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        row.budgetMinor?.let { budget ->
            if (amount != null) {
                Spacer(modifier = Modifier.height(4.dp))
                BudgetProgressBar(
                    spent = amount,
                    limit = budget,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (row.budgetMinor == null && delta != null) {
            val (previousLabel, difference) = delta
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(
                    Res.string.insights_vs_previous,
                    previousLabel,
                    formatTotal(difference, currency),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
