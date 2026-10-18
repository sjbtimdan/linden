package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.pace_collapse
import org.sjbtimdan.linden.resources.pace_daily_allowance
import org.sjbtimdan.linden.resources.pace_expand
import org.sjbtimdan.linden.resources.pace_likely_range
import org.sjbtimdan.linden.resources.pace_on_pace_for
import org.sjbtimdan.linden.resources.pace_usual
import org.sjbtimdan.linden.resources.pace_vs_usual
import org.sjbtimdan.linden.ui.entry.formatAmountCompact
import org.sjbtimdan.linden.ui.insights.PaceForecast
import org.sjbtimdan.linden.ui.theme.CardElevation
import org.sjbtimdan.linden.ui.theme.CardShape
import org.sjbtimdan.linden.ui.theme.lindenColors

/**
 * The current month's Pace: a one-line bar that expands in place to the usual
 * total, the likely range and the remaining daily allowance. The strip is only
 * shown by the ledger for the current month; its expansion state survives
 * rotation but resets when the screen is recreated.
 */
@Composable
fun PaceStrip(forecast: PaceForecast, currency: Currency, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    if (expanded) {
        PaceCard(
            forecast = forecast,
            currency = currency,
            onCollapse = { expanded = false },
            modifier = modifier,
        )
    } else {
        PaceBar(
            forecast = forecast,
            currency = currency,
            onExpand = { expanded = true },
            modifier = modifier,
        )
    }
}

@Composable
private fun PaceBar(forecast: PaceForecast, currency: Currency, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val delta = formatPaceDeltaPercent(forecast.projectedMinor, forecast.typicalMonthMinor)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onExpand)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("paceBar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ShowChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(
                Res.string.pace_on_pace_for,
                amountLabel(forecast.projectedMinor, currency),
            ),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        delta?.let {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.pace_vs_usual, it),
                style = MaterialTheme.typography.labelMedium,
                color = deltaColor(forecast),
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = stringResource(Res.string.pace_expand),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PaceCard(
    forecast: PaceForecast,
    currency: Currency,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val delta = formatPaceDeltaPercent(forecast.projectedMinor, forecast.typicalMonthMinor)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(CardElevation, CardShape, clip = false)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
            .padding(12.dp)
            .testTag("paceCard"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        Res.string.pace_on_pace_for,
                        amountLabel(forecast.projectedMinor, currency),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                delta?.let {
                    Text(
                        text = stringResource(Res.string.pace_vs_usual, it),
                        style = MaterialTheme.typography.labelMedium,
                        color = deltaColor(forecast),
                    )
                }
            }
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Filled.ExpandLess,
                    contentDescription = stringResource(Res.string.pace_collapse),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                Res.string.pace_usual,
                amountLabel(forecast.typicalMonthMinor, currency),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        PaceRangeBar(forecast = forecast)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                Res.string.pace_likely_range,
                amountLabel(forecast.likelyLowMinor, currency),
                amountLabel(forecast.likelyHighMinor, currency),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                Res.string.pace_daily_allowance,
                amountLabel(forecast.dailyAllowanceMinor, currency),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** The spent amount inside the likely range, with ticks for the usual total and the projection. */
@Composable
private fun PaceRangeBar(forecast: PaceForecast, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .testTag("paceRangeBar"),
    ) {
        val max = maxOf(
            forecast.likelyHighMinor,
            forecast.typicalMonthMinor,
            forecast.projectedMinor,
            forecast.spentMinor,
            1L,
        ).toFloat()
        fun fraction(value: Long): Float = (value.coerceAtLeast(0L) / max).coerceIn(0f, 1f)

        val low = fraction(forecast.likelyLowMinor)
        val high = fraction(forecast.likelyHighMinor)
        Box(
            modifier = Modifier
                .offset(x = maxWidth * low)
                .width(maxWidth * (high - low).coerceAtLeast(0f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primaryContainer),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction(forecast.spentMinor))
                .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            modifier = Modifier
                .offset(x = maxWidth * fraction(forecast.typicalMonthMinor))
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.onSurface),
        )
        Box(
            modifier = Modifier
                .offset(x = maxWidth * fraction(forecast.projectedMinor))
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.tertiary),
        )
    }
}

@Composable
private fun deltaColor(forecast: PaceForecast): Color = when {
    forecast.projectedMinor > forecast.typicalMonthMinor -> lindenColors().expense
    forecast.projectedMinor < forecast.typicalMonthMinor -> lindenColors().income
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun amountLabel(amount: Long, currency: Currency): String =
    "${formatAmountCompact(amount, currency.decimalDigits)} ${currency.symbol}"
