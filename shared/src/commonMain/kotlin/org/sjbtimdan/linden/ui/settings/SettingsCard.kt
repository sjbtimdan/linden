package org.sjbtimdan.linden.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sjbtimdan.linden.ui.theme.lindenCard

/** A settings section card on the app's list-row surface (shape, border, shadow). */
@Composable
internal fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .lindenCard(),
        content = content,
    )
}
