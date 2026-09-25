package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.entry_already_added
import org.sjbtimdan.linden.ui.theme.CardElevation
import org.sjbtimdan.linden.ui.theme.CardShape

/**
 * Read-only confirmation of the entry that was just saved, shown above the
 * Add/Clear actions. Styled like a ledger row but inert, so it reads as a
 * receipt of what was added rather than part of the draft above it.
 */
@Composable
fun LastAddedEntry(entry: Entry, modifier: Modifier = Modifier) {
    val tint = entry.tintColor()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(CardElevation, CardShape, clip = false)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("lastAddedEntry"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.entry_already_added),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${entry.title()} · ${entry.subtitle()}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = entry.amountLabel(),
            style = MaterialTheme.typography.titleSmall,
            color = tint,
            maxLines = 1,
        )
    }
}
