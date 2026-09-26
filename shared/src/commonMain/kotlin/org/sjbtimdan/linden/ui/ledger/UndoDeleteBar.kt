package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_dismiss
import org.sjbtimdan.linden.resources.entry_deleted
import org.sjbtimdan.linden.resources.entry_undo
import org.sjbtimdan.linden.ui.entry.HIDDEN_AMOUNT
import org.sjbtimdan.linden.ui.entry.amountLabel
import org.sjbtimdan.linden.ui.entry.subtitle
import org.sjbtimdan.linden.ui.entry.tintColor
import org.sjbtimdan.linden.ui.entry.title
import org.sjbtimdan.linden.ui.theme.CardElevation
import org.sjbtimdan.linden.ui.theme.CardShape

/**
 * Undo offer for the entry most recently deleted from the edit dialog, shown as
 * a row at the bottom of the ledger next to the entry list (like the added-entry
 * receipt on the entry screen). Tapping Undo re-inserts the entry; tapping the
 * leading icon dismisses the offer and makes the delete final; leaving the
 * screen does the same.
 */
@Composable
fun LedgerViewModel.UndoDeleteBar(modifier: Modifier = Modifier) {
    val entry by lastDeleted.collectAsState()
    val hidden by hideTotal.collectAsState()
    // Leaving the screen cancels the offer: the last delete becomes final
    // instead of the bar reappearing on the next visit.
    DisposableEffect(Unit) {
        onDispose { clearLastDeleted() }
    }
    val deleted = entry ?: return
    val tint = deleted.tintColor()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(CardElevation, CardShape, clip = false)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .testTag("undoDeleteBar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = stringResource(Res.string.common_dismiss),
            tint = tint,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = { clearLastDeleted() })
                .padding(8.dp)
                .size(20.dp)
                .testTag("dismissUndoDelete"),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.entry_deleted),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${deleted.title()} · ${deleted.subtitle()}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (hidden) HIDDEN_AMOUNT else deleted.amountLabel(),
            style = MaterialTheme.typography.titleSmall,
            color = tint,
            maxLines = 1,
        )
        TextButton(
            onClick = { undoLastDeleted() },
            modifier = Modifier.testTag("undoDelete"),
        ) {
            Text(stringResource(Res.string.entry_undo))
        }
    }
}
