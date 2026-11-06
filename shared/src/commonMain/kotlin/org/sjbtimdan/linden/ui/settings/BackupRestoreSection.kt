package org.sjbtimdan.linden.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.backup.RestoreResult
import org.sjbtimdan.linden.imports.IvyImportResult
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.settings_backup
import org.sjbtimdan.linden.resources.settings_backup_db
import org.sjbtimdan.linden.resources.settings_backup_failed
import org.sjbtimdan.linden.resources.settings_backup_saved
import org.sjbtimdan.linden.resources.settings_export_csv
import org.sjbtimdan.linden.resources.settings_export_done
import org.sjbtimdan.linden.resources.settings_export_failed
import org.sjbtimdan.linden.resources.settings_import_confirm
import org.sjbtimdan.linden.resources.settings_import_confirm_body
import org.sjbtimdan.linden.resources.settings_import_failed
import org.sjbtimdan.linden.resources.settings_import_ivy
import org.sjbtimdan.linden.resources.settings_import_split_note
import org.sjbtimdan.linden.resources.settings_import_summary
import org.sjbtimdan.linden.resources.settings_restore_backup
import org.sjbtimdan.linden.resources.settings_restore_confirm
import org.sjbtimdan.linden.resources.settings_restore_confirm_body
import org.sjbtimdan.linden.resources.settings_restore_failed
import org.sjbtimdan.linden.resources.settings_restore_summary
import org.sjbtimdan.linden.resources.settings_working_backup
import org.sjbtimdan.linden.resources.settings_working_export
import org.sjbtimdan.linden.resources.settings_working_import
import org.sjbtimdan.linden.resources.settings_working_restore
import org.sjbtimdan.linden.ui.ConfirmDialog

/** The backup, restore, export and import section of the settings screen. */
@Composable
internal fun BackupRestoreSection(
    backupState: BackupState<Unit>,
    restoreState: BackupState<RestoreResult>,
    exportState: BackupState<Unit>,
    importState: BackupState<IvyImportResult>,
    onBackup: () -> Unit,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onImport: () -> Unit,
    onDismissBackup: () -> Unit,
    onDismissRestore: () -> Unit,
    onDismissExport: () -> Unit,
    onDismissImport: () -> Unit,
) {
    var showImportConfirmation by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    val transferInProgress = backupState is BackupState.Working || restoreState is BackupState.Working

    SettingsSectionHeader(stringResource(Res.string.settings_backup))

    SettingsCard {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsActionButton(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = stringResource(Res.string.settings_backup_db),
                onClick = onBackup,
                enabled = !transferInProgress,
            )
            SettingsActionButton(
                icon = Icons.AutoMirrored.Filled.Send,
                label = stringResource(Res.string.settings_export_csv),
                onClick = onExport,
                enabled = exportState !is BackupState.Working,
            )
            SettingsActionButton(
                icon = Icons.Filled.Refresh,
                label = stringResource(Res.string.settings_restore_backup),
                onClick = { showRestoreConfirmation = true },
                enabled = !transferInProgress,
                filled = false,
            )
            SettingsActionButton(
                icon = Icons.AutoMirrored.Filled.Send,
                label = stringResource(Res.string.settings_import_ivy),
                onClick = { showImportConfirmation = true },
                enabled = importState !is BackupState.Working,
                filled = false,
            )
        }
    }

    OperationStatus(
        state = backupState,
        workingText = stringResource(Res.string.settings_working_backup),
        successText = { stringResource(Res.string.settings_backup_saved) },
        failureText = { message -> stringResource(Res.string.settings_backup_failed, message) },
        onDismiss = onDismissBackup,
    )

    OperationStatus(
        state = restoreState,
        workingText = stringResource(Res.string.settings_working_restore),
        successText = { result ->
            stringResource(Res.string.settings_restore_summary, result.accounts, result.categories, result.entries)
        },
        failureText = { message -> stringResource(Res.string.settings_restore_failed, message) },
        onDismiss = onDismissRestore,
    )

    OperationStatus(
        state = exportState,
        workingText = stringResource(Res.string.settings_working_export),
        successText = { stringResource(Res.string.settings_export_done) },
        failureText = { message -> stringResource(Res.string.settings_export_failed, message) },
        onDismiss = onDismissExport,
    )

    OperationStatus(
        state = importState,
        workingText = stringResource(Res.string.settings_working_import),
        successText = { result ->
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
            summary + note
        },
        failureText = { message -> stringResource(Res.string.settings_import_failed, message) },
        onDismiss = onDismissImport,
    )

    if (showImportConfirmation) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_import_ivy),
            body = stringResource(Res.string.settings_import_confirm_body),
            confirmLabel = stringResource(Res.string.settings_import_confirm),
            onConfirm = {
                showImportConfirmation = false
                onImport()
            },
            onDismiss = { showImportConfirmation = false },
        )
    }

    if (showRestoreConfirmation) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_restore_backup),
            body = stringResource(Res.string.settings_restore_confirm_body),
            confirmLabel = stringResource(Res.string.settings_restore_confirm),
            onConfirm = {
                showRestoreConfirmation = false
                onRestore()
            },
            onDismiss = { showRestoreConfirmation = false },
        )
    }
}
