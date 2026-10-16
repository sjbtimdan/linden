package org.sjbtimdan.linden.ui.capture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.capture.CaptureRule
import org.sjbtimdan.linden.capture.CaptureTemplate
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.capture_account_missing
import org.sjbtimdan.linden.resources.capture_add_app
import org.sjbtimdan.linden.resources.capture_apps_empty
import org.sjbtimdan.linden.resources.capture_apps_header
import org.sjbtimdan.linden.resources.capture_apps_note
import org.sjbtimdan.linden.resources.capture_continue
import org.sjbtimdan.linden.resources.capture_disclosure_body
import org.sjbtimdan.linden.resources.capture_disclosure_title
import org.sjbtimdan.linden.resources.capture_enable
import org.sjbtimdan.linden.resources.capture_intro
import org.sjbtimdan.linden.resources.capture_remove_app
import org.sjbtimdan.linden.resources.capture_row_supporting
import org.sjbtimdan.linden.resources.capture_templates_empty
import org.sjbtimdan.linden.resources.capture_templates_header
import org.sjbtimdan.linden.resources.capture_title
import org.sjbtimdan.linden.resources.capture_unsupported
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ConfirmDialog
import org.sjbtimdan.linden.ui.ScreenBackButton
import org.sjbtimdan.linden.ui.screenContainer
import org.sjbtimdan.linden.ui.settings.SettingsCard
import org.sjbtimdan.linden.ui.settings.SettingsSectionHeader

/**
 * Payment-capture settings: the access state (with the disclosure the user
 * must see before the system toggle is opened), the allowlist of apps with
 * their account mappings, and the learned templates the user can forget.
 * Reviewing captures happens in the ledger.
 */
@Composable
fun CaptureSettingsScreen(
    viewModel: CaptureSettingsViewModel,
    access: NotificationAccess,
    capturableApps: List<CapturableApp>,
    onNavigateBack: () -> Unit,
) {
    val rules by viewModel.rules.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showDisclosure by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

    BackHandler(enabled = showDisclosure || showAppPicker) {
        showDisclosure = false
        showAppPicker = false
    }

    Column(
        modifier = Modifier
            .screenContainer()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenBackButton(onClick = onNavigateBack)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.capture_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        when {
            !access.supported -> SettingsCard {
                Text(
                    text = stringResource(Res.string.capture_unsupported),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }

            !access.granted -> SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.capture_intro),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { showDisclosure = true }) {
                        Text(stringResource(Res.string.capture_enable))
                    }
                }
            }

            else -> {
                SettingsSectionHeader(stringResource(Res.string.capture_apps_header))

                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (rules.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.capture_apps_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            )
                        } else {
                            rules.forEachIndexed { index, rule ->
                                if (index > 0) HorizontalDivider()
                                CaptureRuleRow(
                                    rule = rule,
                                    label = capturableApps.firstOrNull { it.packageName == rule.packageName }?.label
                                        ?: rule.packageName,
                                    accounts = accounts,
                                    onAccountSelected = { accountId -> viewModel.setRule(rule.packageName, accountId) },
                                    onEnabledChange = { enabled ->
                                        viewModel.setRuleEnabled(
                                            rule.packageName,
                                            enabled,
                                        )
                                    },
                                    onRemove = { viewModel.removeRule(rule.packageName) },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FilledTonalButton(
                    onClick = { showAppPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(Res.string.capture_add_app))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.capture_apps_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsSectionHeader(stringResource(Res.string.capture_templates_header))

                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (templates.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.capture_templates_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            )
                        } else {
                            templates.forEachIndexed { index, template ->
                                if (index > 0) HorizontalDivider()
                                CaptureTemplateRow(
                                    template = template,
                                    label = capturableApps.firstOrNull { it.packageName == template.packageName }?.label
                                        ?: template.packageName,
                                    categories = categories,
                                    onRemove = { viewModel.removeTemplate(template.packageName, template.skeleton) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDisclosure) {
        ConfirmDialog(
            title = stringResource(Res.string.capture_disclosure_title),
            body = stringResource(Res.string.capture_disclosure_body),
            confirmLabel = stringResource(Res.string.capture_continue),
            onConfirm = {
                showDisclosure = false
                access.openSettings()
            },
            onDismiss = { showDisclosure = false },
        )
    }

    if (showAppPicker) {
        CaptureAppPickerDialog(
            apps = capturableApps.filterNot { app -> rules.any { it.packageName == app.packageName } },
            onPick = { app ->
                showAppPicker = false
                viewModel.setRule(app.packageName, accounts.firstOrNull()?.id ?: 0L)
            },
            onDismiss = { showAppPicker = false },
        )
    }
}

@Composable
private fun CaptureRuleRow(
    rule: CaptureRule,
    label: String,
    accounts: List<Account>,
    onAccountSelected: (Long) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val account = accounts.firstOrNull { it.id == rule.accountId }
    var accountMenuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Box {
                Row(
                    modifier = Modifier.clickable(role = Role.Button) { accountMenuOpen = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = account?.name ?: stringResource(Res.string.capture_account_missing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(expanded = accountMenuOpen, onDismissRequest = { accountMenuOpen = false }) {
                    accounts.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name) },
                            onClick = {
                                accountMenuOpen = false
                                onAccountSelected(option.id)
                            },
                        )
                    }
                }
            }
        }
        Switch(checked = rule.enabled, onCheckedChange = onEnabledChange)
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(Res.string.capture_remove_app),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CaptureTemplateRow(
    template: CaptureTemplate,
    label: String,
    categories: List<Category>,
    onRemove: () -> Unit,
) {
    val category = categories.firstOrNull { it.id == template.categoryId }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template.skeleton,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (category == null) {
                    label
                } else {
                    stringResource(Res.string.capture_row_supporting, label, category.name)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(Res.string.capture_remove_app),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
