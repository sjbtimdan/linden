package org.sjbtimdan.linden.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.first_run_feature_accounts_body
import org.sjbtimdan.linden.resources.first_run_feature_accounts_title
import org.sjbtimdan.linden.resources.first_run_feature_fast_body
import org.sjbtimdan.linden.resources.first_run_feature_fast_title
import org.sjbtimdan.linden.resources.first_run_feature_ledger_body
import org.sjbtimdan.linden.resources.first_run_feature_ledger_title
import org.sjbtimdan.linden.resources.first_run_feature_private_body
import org.sjbtimdan.linden.resources.first_run_feature_private_title
import org.sjbtimdan.linden.resources.first_run_tagline
import org.sjbtimdan.linden.resources.first_run_title

/**
 * The welcome step of the first-run wizard: the app name, a one-line
 * description, and the features that tell a new user what Linden does.
 */
@Composable
fun FirstRunWelcome() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.first_run_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.first_run_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(28.dp))
        FeatureRow(
            icon = Icons.Filled.CurrencyExchange,
            title = stringResource(Res.string.first_run_feature_accounts_title),
            body = stringResource(Res.string.first_run_feature_accounts_body),
        )
        FeatureRow(
            icon = Icons.Filled.Lock,
            title = stringResource(Res.string.first_run_feature_private_title),
            body = stringResource(Res.string.first_run_feature_private_body),
        )
        FeatureRow(
            icon = Icons.Filled.Bolt,
            title = stringResource(Res.string.first_run_feature_fast_title),
            body = stringResource(Res.string.first_run_feature_fast_body),
        )
        FeatureRow(
            icon = Icons.AutoMirrored.Filled.List,
            title = stringResource(Res.string.first_run_feature_ledger_title),
            body = stringResource(Res.string.first_run_feature_ledger_body),
        )
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
