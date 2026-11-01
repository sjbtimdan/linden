package org.sjbtimdan.linden.ui.ledger

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_accounts
import org.sjbtimdan.linden.resources.common_categories
import org.sjbtimdan.linden.resources.ledger_mode_entries
import org.sjbtimdan.linden.ui.theme.lindenColors

private val TabsHeight = 48.dp
private val TabsPadding = 4.dp
private val TabsShape = RoundedCornerShape(22.dp)
private val TabShape = RoundedCornerShape(18.dp)

/**
 * Always-visible switch between the three ledger views (entries, period-end
 * account balances, category totals). Unlike the filters below it, the mode
 * tabs never collapse: they are navigation, not filtering. The selected pill
 * slides between fixed thirds, which leaves more room for the labels than a
 * segmented button row on narrow phones.
 */
@Composable
fun LedgerViewModeTabs(viewMode: LedgerViewMode, onSelect: (LedgerViewMode) -> Unit, modifier: Modifier = Modifier) {
    val modes = LedgerViewMode.entries
    val colors = lindenColors()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("viewModeTabs"),
    ) {
        val itemWidth = (maxWidth - TabsPadding * 2) / modes.size.toFloat()
        val selectedIndex = modes.indexOf(viewMode).coerceAtLeast(0)
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "viewModeIndicator",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TabsHeight)
                .clip(TabsShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(TabsPadding),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(TabShape)
                    .background(Brush.linearGradient(colors.heroGradient)),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                modes.forEach { mode ->
                    val selected = mode == viewMode
                    val labelColor by animateColorAsState(
                        targetValue = if (selected) {
                            colors.heroContent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "viewModeLabelColor",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .requiredHeight(TabsHeight)
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onSelect(mode) },
                            )
                            .testTag("viewModeTab-${mode.name}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = mode.displayName(),
                            style = MaterialTheme.typography.labelLarge,
                            color = labelColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerViewMode.displayName(): String = stringResource(
    when (this) {
        LedgerViewMode.Entries -> Res.string.ledger_mode_entries
        LedgerViewMode.Accounts -> Res.string.common_accounts
        LedgerViewMode.Categories -> Res.string.common_categories
    },
)
