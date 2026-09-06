package org.sjbtimdan.linden.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Stable test tag of the chart container. */
internal const val MONTHLY_TREND_CHART_TAG = "monthlyTrendChart"

/** Test tag prefix of a single month column; full tag is "trendBar-<index>". */
internal const val TREND_BAR_TAG_PREFIX = "trendBar-"

/**
 * One month column of [MonthlyTrendChart]: [label] shows under the bars and
 * [values] holds one value per series, aligned with [MonthlyTrendChart]'s
 * [seriesColors]. [isCurrent] marks the month that is "now", which tints the
 * bars and label.
 */
data class MonthlyTrendBar(
    val label: String,
    val values: List<Long>,
    val isCurrent: Boolean = false,
)

/**
 * Column chart of monthly totals. Each [MonthlyTrendBar] renders one bar per
 * series ([seriesColors] gives their colors, value 0 draws nothing); the
 * tallest bar of any series fills the chart. Tapping a column selects it via
 * [onSelect]. Never charts negative values.
 */
@Composable
fun MonthlyTrendChart(
    bars: List<MonthlyTrendBar>,
    seriesColors: List<Color>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxMinor = bars.maxOfOrNull { bar -> bar.values.maxOrNull() ?: 0L } ?: 0L
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .testTag(MONTHLY_TREND_CHART_TAG),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = barInset),
            horizontalArrangement = Arrangement.spacedBy(columnSpacing),
        ) {
            bars.forEachIndexed { index, bar ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(index) },
                        )
                        .testTag(TREND_BAR_TAG_PREFIX + index),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(subBarGap),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        bar.values.forEachIndexed { seriesIndex, value ->
                            val fraction = barHeightFraction(value, maxMinor)
                            if (fraction > 0f) {
                                val color = seriesColors[seriesIndex].let {
                                    when {
                                        selected -> it
                                        bar.isCurrent -> it.copy(alpha = CURRENT_BAR_ALPHA)
                                        else -> it.copy(alpha = PLAIN_BAR_ALPHA)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(fraction)
                                        .background(
                                            color = color,
                                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                                        ),
                                )
                            } else {
                                // Reserve the slot so zero series keep the bars aligned.
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = barInset),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = barInset, end = barInset, top = labelTopGap),
            horizontalArrangement = Arrangement.spacedBy(columnSpacing),
        ) {
            bars.forEachIndexed { index, bar ->
                val selected = index == selectedIndex
                Text(
                    text = bar.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        selected || bar.isCurrent -> primary
                        else -> onSurfaceVariant
                    },
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Height of a bar as a fraction of the chart's bar area: the tallest value of
 * any series maps to 1f, a value of zero (or an all-zero chart) to 0f.
 * Positive values never shrink below [MIN_VISIBLE_BAR_FRACTION] so tiny bars
 * stay visible.
 */
internal fun barHeightFraction(valueMinor: Long, maxMinor: Long): Float {
    if (valueMinor <= 0L || maxMinor <= 0L) return 0f
    val fraction = valueMinor.toFloat() / maxMinor.toFloat()
    return fraction.coerceIn(MIN_VISIBLE_BAR_FRACTION, 1f)
}

private val chartHeight = 180.dp
private val barInset = 2.dp
private val columnSpacing = 6.dp
private val labelTopGap = 4.dp
private val subBarGap = 2.dp
private const val CURRENT_BAR_ALPHA = 0.45f
private const val PLAIN_BAR_ALPHA = 0.22f
internal const val MIN_VISIBLE_BAR_FRACTION = 0.04f
