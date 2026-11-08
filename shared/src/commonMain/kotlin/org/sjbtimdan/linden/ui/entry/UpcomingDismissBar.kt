package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_dismiss
import org.sjbtimdan.linden.resources.entry_undo
import org.sjbtimdan.linden.resources.entry_upcoming_dismissed
import org.sjbtimdan.linden.ui.theme.lindenCard

/**
 * Undo offer for an upcoming bill dismissed via [onDismiss], shown where the
 * bill used to sit. Tapping Undo restores recurring detection; the leading icon
 * dismisses the offer and makes the dismissal final.
 */
@Composable
fun UpcomingDismissBar(description: String, onUndo: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .lindenCard()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .testTag("upcomingDismissBar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(Res.string.common_dismiss),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onDismiss)
                .padding(8.dp)
                .size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.entry_upcoming_dismissed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(
            onClick = onUndo,
            modifier = Modifier.testTag("undoUpcomingDismiss"),
        ) {
            Text(stringResource(Res.string.entry_undo))
        }
    }
}
