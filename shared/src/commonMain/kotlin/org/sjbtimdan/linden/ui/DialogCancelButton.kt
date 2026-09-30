package org.sjbtimdan.linden.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_cancel

/** Dialog dismiss action labelled with the shared "Cancel" copy. */
@Composable
fun DialogCancelButton(onDismiss: () -> Unit) {
    TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.common_cancel))
    }
}
