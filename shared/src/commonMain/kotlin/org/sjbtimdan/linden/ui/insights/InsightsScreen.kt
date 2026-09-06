package org.sjbtimdan.linden.ui.insights

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_back
import org.sjbtimdan.linden.resources.insights_expenses
import org.sjbtimdan.linden.resources.insights_income
import org.sjbtimdan.linden.resources.insights_next_period
import org.sjbtimdan.linden.resources.insights_previous_period
import org.sjbtimdan.linden.resources.insights_vs_previous
import org.sjbtimdan.linden.ui.ScreenMaxWidth
import org.sjbtimdan.linden.ui.ScreenPadding
import org.sjbtimdan.linden.ui.entry.dateLanguage
import org.sjbtimdan.linden.ui.entry.formatAmountCompact
import org.sjbtimdan.linden.ui.entry.platformLocaleTag
import org.sjbtimdan.linden.ui.ledger.formatTotal
import org.sjbtimdan.linden.ui.screenInsets
import org.sjbtimdan.linden.ui.theme.lindenColors

/**
 * Expense and income chart over a rolling 12-month window. The arrows page
 * the window by twelve months (forward up to the current month); tapping a
 * bar inspects that month, whose totals and change against its predecessor
 * show in the header. The chart language follows the app language like every
 * date in Linden.
 */
@Composable
fun InsightsScreen(viewModel: InsightsViewModel, onNavigateBack: () -> Unit) {
    val months by viewModel.months.collectAsState()
    val windowEnd by viewModel.windowEnd.collectAsState()
    val canStepForward by viewModel.canStepForward.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val hideTotal by viewModel.hideTotal.collectAsState()
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val language = dateLanguage(platformLocaleTag())
    val colors = lindenColors()

    // -1 (or a stale index after paging or data shrank) selects the window's
    // last month; paging resets the selection to the new window's end.
    val effectiveIndex = if (selectedIndex == -1 || selectedIndex >= months.size) {
        months.lastIndex.coerceAtLeast(0)
    } else {
        selectedIndex
    }
    LaunchedEffect(windowEnd) { selectedIndex = -1 }

    Column(
        modifier = Modifier
            .screenInsets()
            .fillMaxSize()
            .padding(ScreenPadding)
            .widthIn(max = ScreenMaxWidth)
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.common_back),
            )
        }

        if (months.isNotEmpty()) {
            val selectedMonth = months[effectiveIndex]
            val previousMonth = months.getOrNull(effectiveIndex - 1)
            val delta = deltaToPrevious(selectedMonth, previousMonth)

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = viewModel::stepBack,
                    modifier = Modifier.testTag("insightsPrevious"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.insights_previous_period),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = language.monthYearText(selectedMonth.monthNumber, selectedMonth.year),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!hideTotal && delta != null && previousMonth != null) {
                        Text(
                            text = stringResource(
                                Res.string.insights_vs_previous,
                                language.monthYearText(previousMonth.monthNumber, previousMonth.year),
                                formatTotal(delta, defaultCurrency),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = viewModel::stepForward,
                    enabled = canStepForward,
                    modifier = Modifier.testTag("insightsNext"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(Res.string.insights_next_period),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            AmountRow(
                label = stringResource(Res.string.insights_expenses),
                dotColor = colors.expense,
                amountColor = colors.expense,
                valueMinor = selectedMonth.expenseMinor,
                currency = defaultCurrency,
                hideTotal = hideTotal,
            )
            AmountRow(
                label = stringResource(Res.string.insights_income),
                dotColor = colors.income,
                amountColor = colors.income,
                valueMinor = selectedMonth.incomeMinor,
                currency = defaultCurrency,
                hideTotal = hideTotal,
            )

            Spacer(modifier = Modifier.height(20.dp))
            MonthlyTrendChart(
                bars = monthlyTrendBars(months, language),
                seriesColors = listOf(colors.expense, colors.income),
                selectedIndex = effectiveIndex,
                onSelect = { selectedIndex = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Selected-month total, e.g. "1'234.56 CHF" (compact above one million). */
internal fun amountLabel(valueMinor: Long, currency: Currency): String =
    "${formatAmountCompact(valueMinor)} ${currency.symbol}"

/** One legend row: a color dot, the series name and its amount for the month. */
@Composable
private fun AmountRow(
    label: String,
    dotColor: Color,
    amountColor: Color,
    valueMinor: Long?,
    currency: Currency,
    hideTotal: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = dotColor, shape = CircleShape),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = when {
                hideTotal -> "••••••"
                valueMinor == null -> "–"
                else -> amountLabel(valueMinor, currency)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
            textAlign = TextAlign.End,
        )
    }
}
