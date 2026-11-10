package org.sjbtimdan.linden.ui.entry

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.entry_hide_total
import org.sjbtimdan.linden.resources.entry_show_total
import org.sjbtimdan.linden.resources.entry_total_balance
import org.sjbtimdan.linden.resources.settings_nav_insights
import org.sjbtimdan.linden.ui.theme.lindenColors

private val HeroShape = RoundedCornerShape(24.dp)
private val HeroElevation = 6.dp
private val HeroPadding = 20.dp
private const val HERO_GLOW_ALPHA = 0.35f
private const val HERO_SHEEN_ALPHA = 0.12f
private const val HERO_LABEL_ALPHA = 0.85f
private const val HERO_BUTTON_ALPHA = 0.16f

/**
 * Total across all accounts in the default currency; null while a rate is
 * missing. The screen's hero: a deep green gradient with soft decorative
 * glows, a large amount and a frosted hide-totals button. [compact] swaps to
 * a slim one-line row while a draft is being captured; [hidden] masks the amount.
 */
@Composable
internal fun TotalBalanceCard(
    total: Long?,
    currency: Currency,
    hidden: Boolean,
    compact: Boolean,
    onToggleHidden: () -> Unit,
    onNavigateToInsights: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = lindenColors()
    val amountParts = when {
        hidden -> HeroAmountParts(whole = HIDDEN_AMOUNT)
        total == null -> HeroAmountParts(whole = "–")
        else -> heroAmountParts(total, currency.decimalDigits)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (compact) "totalBalanceCompact" else "totalBalanceCard")
            .shadow(HeroElevation, HeroShape, clip = false)
            .clip(HeroShape)
            .background(Brush.linearGradient(colors.heroGradient))
            .drawBehind {
                // A warm glow bleeding off the bottom-right corner and a faint
                // sheen from the top edge; both clipped to the card shape.
                val glowCenter = Offset(size.width * 0.98f, size.height * 1.25f)
                val glowRadius = size.minDimension * 0.62f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.heroAccent.copy(alpha = HERO_GLOW_ALPHA),
                            colors.heroAccent.copy(alpha = 0f),
                        ),
                        center = glowCenter,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = glowCenter,
                )
                val sheenCenter = Offset(size.width * 0.85f, -size.height * 0.15f)
                val sheenRadius = size.minDimension * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.heroContent.copy(alpha = HERO_SHEEN_ALPHA),
                            colors.heroContent.copy(alpha = 0f),
                        ),
                        center = sheenCenter,
                        radius = sheenRadius,
                    ),
                    radius = sheenRadius,
                    center = sheenCenter,
                )
            },
    ) {
        if (compact) {
            CompactTotalRow(
                amountParts = amountParts,
                currencySymbol = currency.symbol,
                hidden = hidden,
                contentColor = colors.heroContent,
                accentColor = colors.heroAccent,
                onToggleHidden = onToggleHidden,
            )
        } else {
            ExpandedTotalContent(
                amountParts = amountParts,
                currencySymbol = currency.symbol,
                hidden = hidden,
                contentColor = colors.heroContent,
                accentColor = colors.heroAccent,
                onToggleHidden = onToggleHidden,
                onNavigateToInsights = onNavigateToInsights,
            )
        }
    }
}

@Composable
private fun ExpandedTotalContent(
    amountParts: HeroAmountParts,
    currencySymbol: String,
    hidden: Boolean,
    contentColor: Color,
    accentColor: Color,
    onToggleHidden: () -> Unit,
    onNavigateToInsights: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = HeroPadding, top = 12.dp, end = 10.dp, bottom = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.entry_total_balance),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = HERO_LABEL_ALPHA),
                modifier = Modifier.weight(1f),
            )
            HeroInsightsButton(contentColor = contentColor, onNavigateToInsights = onNavigateToInsights)
            HeroEyeButton(hidden = hidden, contentColor = contentColor, onToggleHidden = onToggleHidden)
        }
        Spacer(modifier = Modifier.height(2.dp))
        AnimatedContent(
            targetState = amountParts,
            transitionSpec = {
                (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { height -> -height / 2 } + fadeOut())
            },
            label = "heroAmount",
        ) { parts ->
            Row {
                HeroAmountText(
                    parts = parts,
                    wholeStyle = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    fractionStyle = MaterialTheme.typography.headlineSmall.copy(
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                    ),
                    contentColor = contentColor,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .alignByBaseline(),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

@Composable
private fun CompactTotalRow(
    amountParts: HeroAmountParts,
    currencySymbol: String,
    hidden: Boolean,
    contentColor: Color,
    accentColor: Color,
    onToggleHidden: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.entry_total_balance),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor.copy(alpha = HERO_LABEL_ALPHA),
            modifier = Modifier.weight(1f),
        )
        AnimatedContent(
            targetState = amountParts,
            transitionSpec = {
                (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { height -> -height / 2 } + fadeOut())
            },
            label = "heroAmountCompact",
        ) { parts ->
            Row {
                HeroAmountText(
                    parts = parts,
                    wholeStyle = MaterialTheme.typography.titleMedium,
                    fractionStyle = MaterialTheme.typography.labelLarge.copy(
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    contentColor = contentColor,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        HeroEyeButton(hidden = hidden, contentColor = contentColor, onToggleHidden = onToggleHidden)
    }
}

@Composable
private fun HeroInsightsButton(contentColor: Color, onNavigateToInsights: () -> Unit) {
    IconButton(
        onClick = onNavigateToInsights,
        modifier = Modifier
            .clip(CircleShape)
            .background(contentColor.copy(alpha = HERO_BUTTON_ALPHA))
            .testTag("entryInsightsAction"),
    ) {
        Icon(
            imageVector = Icons.Filled.BarChart,
            contentDescription = stringResource(Res.string.settings_nav_insights),
            tint = contentColor,
        )
    }
}

@Composable
private fun HeroEyeButton(hidden: Boolean, contentColor: Color, onToggleHidden: () -> Unit) {
    IconButton(
        onClick = onToggleHidden,
        modifier = Modifier
            .clip(CircleShape)
            .background(contentColor.copy(alpha = HERO_BUTTON_ALPHA)),
    ) {
        Icon(
            imageVector = if (hidden) VisibilityOffIcon else VisibilityIcon,
            contentDescription = stringResource(
                if (hidden) Res.string.entry_show_total else Res.string.entry_hide_total,
            ),
            tint = contentColor,
        )
    }
}
