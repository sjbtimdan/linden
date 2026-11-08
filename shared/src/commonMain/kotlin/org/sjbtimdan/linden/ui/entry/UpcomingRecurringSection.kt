package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.predictions.UpcomingRecurring
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_dismiss
import org.sjbtimdan.linden.resources.entry_due
import org.sjbtimdan.linden.resources.entry_upcoming
import org.sjbtimdan.linden.ui.pressFeedback
import org.sjbtimdan.linden.ui.theme.lindenCard

/**
 * Recurring bills due within the next week, shown as a collapsible banner at the
 * top of the form. Tapping a bill prefills the form ([onSelect]) without saving;
 * its close button dismisses it for the session ([onDismiss]). A dismissed bill
 * is re-detected as recurring if it is logged again, so nothing is persisted.
 */
@Composable
fun UpcomingRecurringSection(
    upcoming: List<UpcomingRecurring>,
    zone: TimeZone,
    onSelect: (UpcomingRecurring) -> Unit,
    onDismiss: (UpcomingRecurring) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lindenCard(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = { expanded = !expanded })
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("upcomingHeader"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.entry_upcoming),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = upcoming.size.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            HorizontalDivider()
            upcoming.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider()
                UpcomingRecurringRow(item, zone, onSelect, onDismiss)
            }
        }
    }
}

@Composable
private fun UpcomingRecurringRow(
    item: UpcomingRecurring,
    zone: TimeZone,
    onSelect: (UpcomingRecurring) -> Unit,
    onDismiss: (UpcomingRecurring) -> Unit,
) {
    val tint = item.entry.tintColor()
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressFeedback(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = { onSelect(item) },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.entry.title(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(Res.string.entry_due, formatDate(item.nextDue, zone)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.entry.amountLabel(),
            style = MaterialTheme.typography.titleSmall,
            color = tint,
            maxLines = 1,
        )
        IconButton(
            onClick = { onDismiss(item) },
            modifier = Modifier.testTag("upcomingDismiss-${item.entry.description}"),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(Res.string.common_dismiss),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
