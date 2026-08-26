package org.sjbtimdan.linden.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.backup.rememberDatabaseBackupPicker
import org.sjbtimdan.linden.backup.rememberDatabaseRestorePicker
import org.sjbtimdan.linden.imports.rememberZipFilePicker
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.ScreenMaxWidth
import org.sjbtimdan.linden.ui.ScreenPadding
import org.sjbtimdan.linden.ui.screenInsets
import org.sjbtimdan.linden.ui.theme.DialogShape

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToRates: () -> Unit = {},
    pickImportFile: (() -> Unit)? = null,
    pickBackupFile: (() -> Unit)? = null,
    pickRestoreFile: (() -> Unit)? = null,
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val hideLedgerTotal by viewModel.hideLedgerTotal.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()
    var showImportConfirmation by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }

    val importFilePicker = pickImportFile
        ?: rememberZipFilePicker { input -> input?.let(viewModel::importIvy) }
    val backupFilePicker = pickBackupFile
        ?: rememberDatabaseBackupPicker { output -> output?.let(viewModel::backupTo) }
    val restoreFilePicker = pickRestoreFile
        ?: rememberDatabaseRestorePicker { input -> input?.let(viewModel::restoreFrom) }
    val transferInProgress = backupState is BackupState.Working || restoreState is BackupState.Working

    Column(
        modifier = Modifier
            .screenInsets()
            .fillMaxSize()
            .padding(ScreenPadding)
            .widthIn(max = ScreenMaxWidth)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemeMode.entries.size,
                    ),
                ) {
                    Text(
                        text = mode.displayName(),
                    )
                }
            }
        }

        Text(
            text = "Default currency",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Currency.entries.forEach { entry ->
                FilterChip(
                    selected = defaultCurrency == entry,
                    onClick = { viewModel.setDefaultCurrency(entry) },
                    label = { Text(entry.name) },
                )
            }
        }

        Text(
            text = "Privacy",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Hide ledger total",
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = hideLedgerTotal,
                onCheckedChange = viewModel::setHideLedgerTotal,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onNavigateToCategories) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Categories")
            }
            Button(onClick = onNavigateToAccounts) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Accounts")
            }
            Button(onClick = onNavigateToRates) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Currency rates")
            }
            FilledTonalButton(
                onClick = { showImportConfirmation = true },
                enabled = importState !is ImportState.Importing,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import from Ivy")
            }
        }

        when (val state = importState) {
            ImportState.Idle -> Unit

            ImportState.Importing -> WorkingRow(text = "Importing…")

            is ImportState.Success -> {
                val result = state.result
                val note = if (result.splitTransactions > 0) {
                    "\nNote: ${result.splitTransactions} transaction(s) were in a different currency " +
                        "than their account and were imported into new \"IVY: …\" accounts."
                } else {
                    ""
                }
                ImportResultRow(
                    text = "Imported ${result.accounts} accounts, " +
                        "${result.categories} categories, " +
                        "${result.transactions} transactions" + note,
                    onDismiss = viewModel::clearImportState,
                )
            }

            is ImportState.Error -> ImportResultRow(
                text = "Import failed: ${state.message}",
                onDismiss = viewModel::clearImportState,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Backup",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = { backupFilePicker() },
                enabled = !transferInProgress,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back up database")
            }
            OutlinedButton(
                onClick = { showRestoreConfirmation = true },
                enabled = !transferInProgress,
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore from backup")
            }
        }

        when (val state = backupState) {
            BackupState.Idle -> Unit

            BackupState.Working -> WorkingRow(text = "Backing up…")

            is BackupState.Success -> ImportResultRow(
                text = "Backup saved.",
                onDismiss = viewModel::clearBackupState,
            )

            is BackupState.Error -> ImportResultRow(
                text = "Backup failed: ${state.message}",
                onDismiss = viewModel::clearBackupState,
            )
        }

        when (val state = restoreState) {
            BackupState.Idle -> Unit

            BackupState.Working -> WorkingRow(text = "Restoring…")

            is BackupState.Success -> ImportResultRow(
                text = "Restored ${state.value.accounts} accounts, " +
                    "${state.value.categories} categories, " +
                    "${state.value.entries} entries",
                onDismiss = viewModel::clearRestoreState,
            )

            is BackupState.Error -> ImportResultRow(
                text = "Restore failed: ${state.message}",
                onDismiss = viewModel::clearRestoreState,
            )
        }

        if (showImportConfirmation) {
            AlertDialog(
                onDismissRequest = { showImportConfirmation = false },
                shape = DialogShape,
                title = { Text("Import from Ivy") },
                text = { Text("This will replace all your current accounts, categories and transactions. Continue?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showImportConfirmation = false
                            importFilePicker()
                        },
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showImportConfirmation = false },
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }

        if (showRestoreConfirmation) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirmation = false },
                shape = DialogShape,
                title = { Text("Restore from backup") },
                text = {
                    Text(
                        "This will replace all your current accounts, categories, transactions and settings. Continue?",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showRestoreConfirmation = false
                            restoreFilePicker()
                        },
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showRestoreConfirmation = false },
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
private fun WorkingRow(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ImportResultRow(text: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onDismiss,
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Dismiss",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
