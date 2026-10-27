package org.sjbtimdan.linden.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.AppLanguage
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.capture_enable
import org.sjbtimdan.linden.resources.capture_intro
import org.sjbtimdan.linden.resources.capture_title
import org.sjbtimdan.linden.resources.first_run_capture_allowed
import org.sjbtimdan.linden.resources.first_run_capture_find
import org.sjbtimdan.linden.resources.first_run_capture_later
import org.sjbtimdan.linden.resources.first_run_continue
import org.sjbtimdan.linden.resources.first_run_subtitle
import org.sjbtimdan.linden.resources.first_run_title
import org.sjbtimdan.linden.resources.settings_default_currency
import org.sjbtimdan.linden.resources.settings_language
import org.sjbtimdan.linden.resources.settings_theme
import org.sjbtimdan.linden.ui.capture.NotificationAccess
import org.sjbtimdan.linden.ui.capture.rememberNotificationAccess
import org.sjbtimdan.linden.ui.entry.platformLocaleTag
import org.sjbtimdan.linden.ui.settings.SettingsSectionHeader
import org.sjbtimdan.linden.ui.theme.LindenTheme

/**
 * Shown once on a fresh install, before the app: asks for the app language,
 * default currency and theme so the app starts in a setup the user recognizes.
 * The language previews live — picking one re-composes the screen in it — and
 * the theme previews the same way. Nothing is persisted until Continue, which
 * passes all three choices to [onComplete]; the resolved system language, CHF
 * and the System theme are pre-selected so Continue is always enabled.
 *
 * Where the platform has notification capture (Android), the same screen
 * introduces it and offers the system grant. Which apps may be read is left to
 * Settings: a capture rule binds an app to an account, and no accounts exist
 * until Continue seeds them. Capture is never a gate — Continue stays enabled.
 */
@Composable
fun FirstRunScreen(
    access: NotificationAccess = rememberNotificationAccess(),
    onComplete: (AppLanguage, Currency, ThemeMode) -> Unit,
) {
    var language by rememberSaveable {
        mutableStateOf(AppLanguage.resolve(AppLanguage.SYSTEM, platformLocaleTag()))
    }
    var currency by rememberSaveable { mutableStateOf(Currency.CHF) }
    var theme by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }

    ApplyLanguageOverride(language)
    key(language) {
        LindenTheme(themeMode = theme) {
            Column(
                modifier = Modifier
                    .screenContainer()
                    .verticalScroll(rememberScrollState())
                    .testTag("firstRunScreen"),
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(Res.string.first_run_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.first_run_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SettingsSectionHeader(stringResource(Res.string.settings_language))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    languageOptions.forEach { option ->
                        FilterChip(
                            selected = language == option,
                            onClick = { language = option },
                            label = { Text(option.label()) },
                            modifier = Modifier.testTag("firstRunLanguage-${option.name}"),
                        )
                    }
                }

                SettingsSectionHeader(stringResource(Res.string.settings_default_currency))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Currency.entries.forEach { entry ->
                        FilterChip(
                            selected = currency == entry,
                            onClick = { currency = entry },
                            label = { Text(entry.name) },
                            modifier = Modifier.testTag("firstRunCurrency-${entry.name}"),
                        )
                    }
                }

                SettingsSectionHeader(stringResource(Res.string.settings_theme))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = theme == mode,
                            onClick = { theme = mode },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size,
                            ),
                            modifier = Modifier.testTag("firstRunTheme-${mode.name}"),
                        ) {
                            Text(mode.displayName())
                        }
                    }
                }

                if (access.supported) {
                    SettingsSectionHeader(stringResource(Res.string.capture_title))
                    Text(
                        text = stringResource(Res.string.capture_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (access.granted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.first_run_capture_allowed),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.testTag("firstRunCaptureAllowed"),
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = access.openSettings,
                            modifier = Modifier.testTag("firstRunCaptureAccess"),
                        ) {
                            Text(stringResource(Res.string.capture_enable))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.first_run_capture_find),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("firstRunCaptureFind"),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.first_run_capture_later),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { onComplete(language, currency, theme) },
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("firstRunContinue"),
                ) {
                    Text(stringResource(Res.string.first_run_continue))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
