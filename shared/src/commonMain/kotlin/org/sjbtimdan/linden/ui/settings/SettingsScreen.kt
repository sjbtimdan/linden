package org.sjbtimdan.linden.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.BuildInfo
import org.sjbtimdan.linden.backup.rememberDatabaseBackupPicker
import org.sjbtimdan.linden.backup.rememberDatabaseRestorePicker
import org.sjbtimdan.linden.export.rememberCsvExportPicker
import org.sjbtimdan.linden.imports.rememberZipFilePicker
import org.sjbtimdan.linden.model.AppLanguage
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.capture_title
import org.sjbtimdan.linden.resources.common_accounts
import org.sjbtimdan.linden.resources.common_categories
import org.sjbtimdan.linden.resources.nav_settings
import org.sjbtimdan.linden.resources.settings_default_currency
import org.sjbtimdan.linden.resources.settings_hide_totals
import org.sjbtimdan.linden.resources.settings_language
import org.sjbtimdan.linden.resources.settings_nav_budgets
import org.sjbtimdan.linden.resources.settings_nav_insights
import org.sjbtimdan.linden.resources.settings_nav_rates
import org.sjbtimdan.linden.resources.settings_privacy
import org.sjbtimdan.linden.resources.settings_theme
import org.sjbtimdan.linden.resources.settings_your_data
import org.sjbtimdan.linden.time.SystemClock
import org.sjbtimdan.linden.ui.BottomBarContentSpacer
import org.sjbtimdan.linden.ui.ThemeModePicker
import org.sjbtimdan.linden.ui.entry.platformLocaleTag
import org.sjbtimdan.linden.ui.label
import org.sjbtimdan.linden.ui.languageOptions
import org.sjbtimdan.linden.ui.screenContainer

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToRates: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToCapture: () -> Unit = {},
    pickImportFile: (() -> Unit)? = null,
    pickBackupFile: (() -> Unit)? = null,
    pickRestoreFile: (() -> Unit)? = null,
    pickExportFile: (() -> Unit)? = null,
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val language by viewModel.language.collectAsState()
    val hideEntryTotal by viewModel.hideEntryTotal.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    val importFilePicker = pickImportFile
        ?: rememberZipFilePicker { input -> input?.let(viewModel::importIvy) }
    val backupFilePicker = pickBackupFile
        ?: rememberDatabaseBackupPicker(onPicked = { output -> output?.let(viewModel::backupTo) }, clock = SystemClock)
    val restoreFilePicker = pickRestoreFile
        ?: rememberDatabaseRestorePicker { input -> input?.let(viewModel::restoreFrom) }
    val exportFilePicker = pickExportFile
        ?: rememberCsvExportPicker { output -> output?.let(viewModel::exportCsv) }

    Column(
        modifier = Modifier
            .screenContainer()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(Res.string.nav_settings),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        SettingsSectionHeader(stringResource(Res.string.settings_language))

        // No "follow system" option: the selected chip is the concrete language
        // in effect, which resolves from the platform locale until the user
        // pins one (unsupported system languages fall back to English).
        val effectiveLanguage = AppLanguage.resolve(language, platformLocaleTag())

        SettingsCard {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                languageOptions.forEach { option ->
                    FilterChip(
                        selected = effectiveLanguage == option,
                        onClick = { viewModel.setLanguage(option) },
                        label = { Text(option.label()) },
                        modifier = Modifier.testTag("language-${option.name}"),
                    )
                }
            }
        }

        SettingsSectionHeader(stringResource(Res.string.settings_your_data))

        SettingsCard {
            SettingsNavRow(
                icon = Icons.AutoMirrored.Filled.List,
                label = stringResource(Res.string.common_categories),
                onClick = onNavigateToCategories,
            )
            HorizontalDivider()
            SettingsNavRow(
                icon = Icons.Filled.Person,
                label = stringResource(Res.string.common_accounts),
                onClick = onNavigateToAccounts,
            )
            HorizontalDivider()
            SettingsNavRow(
                icon = Icons.Filled.CurrencyExchange,
                label = stringResource(Res.string.settings_nav_rates),
                onClick = onNavigateToRates,
            )
            HorizontalDivider()
            SettingsNavRow(
                icon = Icons.Filled.Savings,
                label = stringResource(Res.string.settings_nav_budgets),
                onClick = onNavigateToBudgets,
            )
            HorizontalDivider()
            SettingsNavRow(
                icon = Icons.Filled.BarChart,
                label = stringResource(Res.string.settings_nav_insights),
                onClick = onNavigateToInsights,
            )
            HorizontalDivider()
            SettingsNavRow(
                icon = Icons.Filled.Notifications,
                label = stringResource(Res.string.capture_title),
                onClick = onNavigateToCapture,
            )
        }

        SettingsSectionHeader(stringResource(Res.string.settings_theme))

        SettingsCard {
            ThemeModePicker(
                selected = themeMode,
                onSelect = viewModel::setThemeMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            )
        }

        SettingsSectionHeader(stringResource(Res.string.settings_default_currency))

        SettingsCard {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
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
        }

        SettingsSectionHeader(stringResource(Res.string.settings_privacy))

        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
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
        }

        BackupRestoreSection(
            backupState = backupState,
            restoreState = restoreState,
            exportState = exportState,
            importState = importState,
            onBackup = { backupFilePicker() },
            onExport = { exportFilePicker() },
            onRestore = { restoreFilePicker() },
            onImport = { importFilePicker() },
            onDismissBackup = viewModel::clearBackupState,
            onDismissRestore = viewModel::clearRestoreState,
            onDismissExport = viewModel::clearExportState,
            onDismissImport = viewModel::clearImportState,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = buildVersionLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        BottomBarContentSpacer()
    }
}

private fun buildVersionLabel(): String {
    val commit = if (BuildInfo.GIT_TAGGED) "" else " (${BuildInfo.GIT_COMMIT})"
    return "Linden v${BuildInfo.VERSION}$commit"
}
