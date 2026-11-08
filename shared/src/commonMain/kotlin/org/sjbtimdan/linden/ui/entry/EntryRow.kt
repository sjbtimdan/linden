package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.entry_type_transfer
import org.sjbtimdan.linden.ui.pressFeedback
import org.sjbtimdan.linden.ui.theme.accentColor
import org.sjbtimdan.linden.ui.theme.accentContentColor
import org.sjbtimdan.linden.ui.theme.lindenCard
import org.sjbtimdan.linden.ui.theme.lindenColors

@Composable
internal fun Entry.title(): String {
    val description = description?.takeIf { it.isNotBlank() }
    return when (this) {
        is TransferEntry -> description ?: stringResource(Res.string.entry_type_transfer)
        else -> description ?: requireNotNull(category).name
    }
}

internal fun Entry.subtitle(): String = when (this) {
    is TransferEntry -> "${account.name} → ${toAccount.name}"
    else -> account.name
}

/** The single secondary line: the account (or transfer pair) with the time appended. */
internal fun Entry.subtitleLine(showTime: Boolean, zone: TimeZone): String {
    val subtitle = subtitle()
    return if (showTime) "$subtitle · ${formatTime(createdAt, zone)}" else subtitle
}

internal fun Entry.amountLabel(): String = when (type) {
    EntryType.Expense -> "− ${formatAmountCompact(amount, account.currency.decimalDigits)} ${account.currency.symbol}"
    EntryType.Income -> "+ ${formatAmountCompact(amount, account.currency.decimalDigits)} ${account.currency.symbol}"
    EntryType.Transfer -> "${formatAmountCompact(amount, account.currency.decimalDigits)} ${account.currency.symbol}"
}

@Composable
internal fun Entry.tintColor(): Color = when (type) {
    EntryType.Expense -> lindenColors().expense
    EntryType.Income -> lindenColors().income
    EntryType.Transfer -> lindenColors().transfer
}

/**
 * A single entry row, colored by its category accent (transfers, which carry
 * none, fall back to the entry-type tint): a solid icon disc leads, while the
 * amount keeps the type tint so the direction of the money stays readable.
 * Two text lines: the title, then the account (or transfer pair). [showTime]
 * appends the time-of-day to that secondary line: rows are grouped under day
 * headers that already carry the date, so it only tells same-day entries
 * apart. [hideAmounts] masks the amount while "Hide Totals" is on, leaving the
 * text readable.
 */
@Composable
fun EntryRow(
    entry: Entry,
    onClick: () -> Unit,
    showTime: Boolean = true,
    hideAmounts: Boolean = false,
    zone: TimeZone = TimeZone.currentSystemDefault(),
    modifier: Modifier = Modifier,
) {
    val tint = entry.tintColor()
    val accent = entry.category?.let { accentColor(it.name) } ?: tint
    val accentContent = accentContentColor(accent)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressFeedback(interactionSource)
            .lindenCard()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.category?.icon?.imageVector() ?: entry.type.icon(),
                contentDescription = null,
                tint = accentContent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title(),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = entry.subtitleLine(showTime, zone),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (hideAmounts) HIDDEN_AMOUNT else entry.amountLabel(),
            style = MaterialTheme.typography.titleMedium,
            color = tint,
            maxLines = 1,
        )
    }
}
