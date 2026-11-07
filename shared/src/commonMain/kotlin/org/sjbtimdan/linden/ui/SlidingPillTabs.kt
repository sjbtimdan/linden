package org.sjbtimdan.linden.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.ui.theme.lindenColors

private val PillHeight = 48.dp
private val PillPadding = 4.dp
private val PillContainerShape = RoundedCornerShape(22.dp)
private val PillIndicatorShape = RoundedCornerShape(18.dp)

/** One sliding-pill tab: the visible label, an optional leading icon and a tag for tests. */
data class SlidingPillTab(
    val label: String,
    val icon: ImageVector? = null,
    val tag: String? = null,
)

/**
 * A switcher rendered as the brand sliding pill: a gradient indicator slides
 * between fixed equal thirds and the selected label sits on it, ahead of any
 * ordinary segmented button row. With the [SlidingPillTab.tag] set, each tab
 * carries `testTag` for tests and `Role.Tab` selection semantics.
 */
@Composable
fun SlidingPillTabs(
    tabs: List<SlidingPillTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = lindenColors()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(PillHeight),
    ) {
        val itemWidth = (maxWidth - PillPadding * 2) / tabs.size.toFloat()
        val selected = selectedIndex.coerceIn(tabs.indices)
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selected,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "slidingPillIndicator",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PillHeight)
                .clip(PillContainerShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(PillPadding),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(PillIndicatorShape)
                    .background(Brush.linearGradient(colors.heroGradient)),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isTabSelected = index == selected
                    val labelColor by animateColorAsState(
                        targetValue = if (isTabSelected) {
                            colors.heroContent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "slidingPillLabelColor",
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .requiredHeight(PillHeight)
                            .selectable(
                                selected = isTabSelected,
                                role = Role.Tab,
                                onClick = { onSelect(index) },
                            )
                            .then(if (tab.tag != null) Modifier.testTag(tab.tag) else Modifier),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tab.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = labelColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = tab.label,
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
