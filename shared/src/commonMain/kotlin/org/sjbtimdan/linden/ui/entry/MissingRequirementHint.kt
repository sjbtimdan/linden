package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Error-colored line naming the field that keeps the draft from being saved.
 * When [onClick] is given (the blocker is a missing category or account that
 * must be created), it renders as an action button that opens the create dialog
 * instead of inert text.
 */
@Composable
fun MissingRequirementHint(message: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    if (onClick != null) {
        TextButton(
            onClick = onClick,
            modifier = modifier.testTag("missingRequirementAction"),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
            )
        }
    } else {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
    }
}
