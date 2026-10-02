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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.entry_hide_total
import org.sjbtimdan.linden.resources.entry_show_total
import org.sjbtimdan.linden.resources.entry_total_balance
import org.sjbtimdan.linden.ui.theme.lindenColors

private val HeroShape = RoundedCornerShape(24.dp)
private val HeroBadgeShape = RoundedCornerShape(8.dp)
private val HeroElevation = 6.dp
private val HeroPadding = 20.dp
private const val HERO_GLOW_ALPHA = 0.35f
private const val HERO_SHEEN_ALPHA = 0.12f
private const val HERO_LABEL_ALPHA = 0.85f
private const val HERO_BUTTON_ALPHA = 0.16f
private const val HERO_BADGE_ALPHA = 0.18f

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
    modifier: Modifier = Modifier,
) {
    val colors = lindenColors()
    val amountLabel = if (hidden) {
        HIDDEN_AMOUNT
    } else {
        total?.let { formatAmountCompact(it, currency.decimalDigits) } ?: "–"
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
                amountLabel = amountLabel,
                currencySymbol = currency.symbol,
                hidden = hidden,
                contentColor = colors.heroContent,
                onToggleHidden = onToggleHidden,
            )
        } else {
            ExpandedTotalContent(
                amountLabel = amountLabel,
                currencySymbol = currency.symbol,
                hidden = hidden,
                contentColor = colors.heroContent,
                onToggleHidden = onToggleHidden,
            )
        }
    }
}

@Composable
private fun ExpandedTotalContent(
    amountLabel: String,
    currencySymbol: String,
    hidden: Boolean,
    contentColor: Color,
    onToggleHidden: () -> Unit,
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
            HeroEyeButton(hidden = hidden, contentColor = contentColor, onToggleHidden = onToggleHidden)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedContent(
                targetState = amountLabel,
                transitionSpec = {
                    (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -height / 2 } + fadeOut())
                },
                label = "heroAmount",
                modifier = Modifier.weight(1f, fill = false),
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            CurrencyBadge(
                symbol = currencySymbol,
                contentColor = contentColor,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun CompactTotalRow(
    amountLabel: String,
    currencySymbol: String,
    hidden: Boolean,
    contentColor: Color,
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
            targetState = amountLabel,
            transitionSpec = {
                (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { height -> -height / 2 } + fadeOut())
            },
            label = "heroAmountCompact",
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = currencySymbol,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor.copy(alpha = HERO_LABEL_ALPHA),
        )
        HeroEyeButton(hidden = hidden, contentColor = contentColor, onToggleHidden = onToggleHidden)
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

@Composable
private fun CurrencyBadge(symbol: String, contentColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = HeroBadgeShape,
        color = contentColor.copy(alpha = HERO_BADGE_ALPHA),
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
