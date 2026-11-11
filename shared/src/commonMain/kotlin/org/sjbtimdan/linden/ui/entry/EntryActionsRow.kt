package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_clear
import org.sjbtimdan.linden.resources.common_save
import org.sjbtimdan.linden.resources.entry_add

/** The form's Add and Clear pair; Add reads "Save" while updating an entry. */
@Composable
internal fun EntryActionsRow(draft: EntryDraft?, addEnabled: Boolean, onAdd: () -> Unit, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onAdd,
            enabled = addEnabled,
            modifier = Modifier.weight(1f).testTag("saveEntry"),
        ) {
            Icon(
                imageVector = if (draft?.editing != null) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                stringResource(
                    if (draft?.editing != null) Res.string.common_save else Res.string.entry_add,
                ),
            )
        }

        OutlinedButton(
            onClick = onClear,
            enabled = draft != null,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(Res.string.common_clear))
        }
    }
}
