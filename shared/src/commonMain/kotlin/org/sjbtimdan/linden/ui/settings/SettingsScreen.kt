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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.BuildInfo
import org.sjbtimdan.linden.backup.rememberDatabaseBackupPicker
import org.sjbtimdan.linden.backup.rememberDatabaseRestorePicker
import org.sjbtimdan.linden.export.rememberCsvExportPicker
import org.sjbtimdan.linden.imports.rememberZipFilePicker
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_cancel
import org.sjbtimdan.linden.resources.common_dismiss
import org.sjbtimdan.linden.resources.common_unknown_error
import org.sjbtimdan.linden.resources.settings_backup
import org.sjbtimdan.linden.resources.settings_backup_db
import org.sjbtimdan.linden.resources.settings_backup_failed
import org.sjbtimdan.linden.resources.settings_backup_saved
import org.sjbtimdan.linden.resources.settings_default_currency
import org.sjbtimdan.linden.resources.settings_export_csv
import org.sjbtimdan.linden.resources.settings_export_done
import org.sjbtimdan.linden.resources.settings_export_failed
import org.sjbtimdan.linden.resources.settings_hide_totals
import org.sjbtimdan.linden.resources.settings_import_confirm
import org.sjbtimdan.linden.resources.settings_import_confirm_body
import org.sjbtimdan.linden.resources.settings_import_failed
import org.sjbtimdan.linden.resources.settings_import_ivy
import org.sjbtimdan.linden.resources.settings_import_split_note
import org.sjbtimdan.linden.resources.settings_import_summary
import org.sjbtimdan.linden.resources.settings_nav_accounts
import org.sjbtimdan.linden.resources.settings_nav_budgets
import org.sjbtimdan.linden.resources.settings_nav_categories
import org.sjbtimdan.linden.resources.settings_nav_rates
import org.sjbtimdan.linden.resources.settings_privacy
import org.sjbtimdan.linden.resources.settings_restore_backup
import org.sjbtimdan.linden.resources.settings_restore_confirm
import org.sjbtimdan.linden.resources.settings_restore_confirm_body
import org.sjbtimdan.linden.resources.settings_restore_failed
import org.sjbtimdan.linden.resources.settings_restore_summary
import org.sjbtimdan.linden.resources.settings_theme
import org.sjbtimdan.linden.resources.settings_theme_dark
import org.sjbtimdan.linden.resources.settings_theme_light
import org.sjbtimdan.linden.resources.settings_theme_system
import org.sjbtimdan.linden.resources.settings_working_backup
import org.sjbtimdan.linden.resources.settings_working_export
import org.sjbtimdan.linden.resources.settings_working_import
import org.sjbtimdan.linden.resources.settings_working_restore
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
    onNavigateToBudgets: () -> Unit = {},
    pickImportFile: (() -> Unit)? = null,
    pickBackupFile: (() -> Unit)? = null,
    pickRestoreFile: (() -> Unit)? = null,
    pickExportFile: (() -> Unit)? = null,
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val hideEntryTotal by viewModel.hideEntryTotal.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    var showImportConfirmation by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }

    val importFilePicker = pickImportFile
        ?: rememberZipFilePicker { input -> input?.let(viewModel::importIvy) }
    val backupFilePicker = pickBackupFile
        ?: rememberDatabaseBackupPicker { output -> output?.let(viewModel::backupTo) }
    val restoreFilePicker = pickRestoreFile
        ?: rememberDatabaseRestorePicker { input -> input?.let(viewModel::restoreFrom) }
    val exportFilePicker = pickExportFile
        ?: rememberCsvExportPicker { output -> output?.let(viewModel::exportCsv) }
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
            text = stringResource(Res.string.settings_theme),
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
            text = stringResource(Res.string.settings_default_currency),
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
            text = stringResource(Res.string.settings_privacy),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.settings_hide_totals),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = hideEntryTotal,
                onCheckedChange = viewModel::setHideEntryTotal,
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
                Text(stringResource(Res.string.settings_nav_categories))
            }
            Button(onClick = onNavigateToAccounts) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(Res.string.settings_nav_accounts))
            }
            Button(onClick = onNavigateToRates) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(Res.string.settings_nav_rates))
            }
            Button(onClick = onNavigateToBudgets) {
                Icon(
                    imageVector = Icons.Filled.Savings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(Res.string.settings_nav_budgets))
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
                Text(stringResource(Res.string.settings_import_ivy))
            }
        }

        when (val state = importState) {
            ImportState.Idle -> Unit

            ImportState.Importing -> WorkingRow(text = stringResource(Res.string.settings_working_import))

            is ImportState.Success -> {
                val result = state.result
                val summary = stringResource(
                    Res.string.settings_import_summary,
                    result.accounts,
                    result.categories,
                    result.transactions,
                )
                val note = if (result.splitTransactions > 0) {
                    stringResource(Res.string.settings_import_split_note, result.splitTransactions)
                } else {
                    ""
                }
                ImportResultRow(
                    text = summary + note,
                    onDismiss = viewModel::clearImportState,
                )
            }

            is ImportState.Error -> ImportResultRow(
                text = stringResource(
                    Res.string.settings_import_failed,
                    state.message ?: stringResource(Res.string.common_unknown_error),
                ),
                onDismiss = viewModel::clearImportState,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.settings_backup),
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
                Text(stringResource(Res.string.settings_backup_db))
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
                Text(stringResource(Res.string.settings_restore_backup))
            }
            OutlinedButton(
                onClick = { exportFilePicker() },
                enabled = exportState !is BackupState.Working,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(Res.string.settings_export_csv))
            }
        }

        when (val state = backupState) {
            BackupState.Idle -> Unit

            BackupState.Working -> WorkingRow(text = stringResource(Res.string.settings_working_backup))

            is BackupState.Success -> ImportResultRow(
                text = stringResource(Res.string.settings_backup_saved),
                onDismiss = viewModel::clearBackupState,
            )

            is BackupState.Error -> ImportResultRow(
                text = stringResource(
                    Res.string.settings_backup_failed,
                    state.message ?: stringResource(Res.string.common_unknown_error),
                ),
                onDismiss = viewModel::clearBackupState,
            )
        }

        when (val state = restoreState) {
            BackupState.Idle -> Unit

            BackupState.Working -> WorkingRow(text = stringResource(Res.string.settings_working_restore))

            is BackupState.Success -> ImportResultRow(
                text = stringResource(
                    Res.string.settings_restore_summary,
                    state.value.accounts,
                    state.value.categories,
                    state.value.entries,
                ),
                onDismiss = viewModel::clearRestoreState,
            )

            is BackupState.Error -> ImportResultRow(
                text = stringResource(
                    Res.string.settings_restore_failed,
                    state.message ?: stringResource(Res.string.common_unknown_error),
                ),
                onDismiss = viewModel::clearRestoreState,
            )
        }

        when (val state = exportState) {
            BackupState.Idle -> Unit

            BackupState.Working -> WorkingRow(text = stringResource(Res.string.settings_working_export))

            is BackupState.Success -> ImportResultRow(
                text = stringResource(Res.string.settings_export_done),
                onDismiss = viewModel::clearExportState,
            )

            is BackupState.Error -> ImportResultRow(
                text = stringResource(
                    Res.string.settings_export_failed,
                    state.message ?: stringResource(Res.string.common_unknown_error),
                ),
                onDismiss = viewModel::clearExportState,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = buildVersionLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (showImportConfirmation) {
            AlertDialog(
                onDismissRequest = { showImportConfirmation = false },
                shape = DialogShape,
                title = { Text(stringResource(Res.string.settings_import_ivy)) },
                text = { Text(stringResource(Res.string.settings_import_confirm_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showImportConfirmation = false
                            importFilePicker()
                        },
                    ) {
                        Text(stringResource(Res.string.settings_import_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showImportConfirmation = false },
                    ) {
                        Text(stringResource(Res.string.common_cancel))
                    }
                },
            )
        }

        if (showRestoreConfirmation) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirmation = false },
                shape = DialogShape,
                title = { Text(stringResource(Res.string.settings_restore_backup)) },
                text = { Text(stringResource(Res.string.settings_restore_confirm_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showRestoreConfirmation = false
                            restoreFilePicker()
                        },
                    ) {
                        Text(stringResource(Res.string.settings_restore_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showRestoreConfirmation = false },
                    ) {
                        Text(stringResource(Res.string.common_cancel))
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(Res.string.common_dismiss),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ThemeMode.displayName(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> Res.string.settings_theme_system
        ThemeMode.LIGHT -> Res.string.settings_theme_light
        ThemeMode.DARK -> Res.string.settings_theme_dark
    },
)

private fun buildVersionLabel(): String {
    val dirty = if (BuildInfo.GIT_DIRTY) " (dirty)" else ""
    return "Linden v${BuildInfo.VERSION} (${BuildInfo.GIT_COMMIT}$dirty)"
}
