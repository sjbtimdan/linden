package org.sjbtimdan.linden.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import org.sjbtimdan.linden.resources.common_back
import org.sjbtimdan.linden.resources.first_run_capture_allowed
import org.sjbtimdan.linden.resources.first_run_capture_choose_now
import org.sjbtimdan.linden.resources.first_run_capture_find
import org.sjbtimdan.linden.resources.first_run_capture_how
import org.sjbtimdan.linden.resources.first_run_capture_later
import org.sjbtimdan.linden.resources.first_run_capture_step_access
import org.sjbtimdan.linden.resources.first_run_capture_step_add
import org.sjbtimdan.linden.resources.first_run_capture_step_finish
import org.sjbtimdan.linden.resources.first_run_choose_currency
import org.sjbtimdan.linden.resources.first_run_choose_language
import org.sjbtimdan.linden.resources.first_run_choose_theme
import org.sjbtimdan.linden.resources.first_run_continue
import org.sjbtimdan.linden.resources.first_run_get_started
import org.sjbtimdan.linden.ui.capture.NotificationAccess
import org.sjbtimdan.linden.ui.capture.rememberNotificationAccess
import org.sjbtimdan.linden.ui.entry.platformLocaleTag
import org.sjbtimdan.linden.ui.theme.LindenTheme

/** The ordered steps of the first-run wizard. */
internal enum class FirstRunStep { Language, Welcome, Currency, Theme, Capture }

/** The wizard steps; payment capture is only offered where the platform supports it. */
internal fun firstRunSteps(captureSupported: Boolean): List<FirstRunStep> = buildList {
    add(FirstRunStep.Language)
    add(FirstRunStep.Welcome)
    add(FirstRunStep.Currency)
    add(FirstRunStep.Theme)
    if (captureSupported) add(FirstRunStep.Capture)
}

/**
 * Shown once on a fresh install, before the app: a short wizard that starts
 * with the app language so every later step renders in the user's choice,
 * then introduces Linden and asks for the default currency and theme so the
 * app starts in a setup the user recognizes. The language and theme preview
 * live — picking one re-composes every step in it — and nothing is persisted
 * until the last step's Continue, which passes all three choices to
 * [onComplete]; the resolved system language, CHF and the System theme are
 * pre-selected so the wizard always reaches the end.
 *
 * Where the platform has notification capture (Android), a dedicated step
 * introduces it, explains the three steps to turn it on and offers the system
 * grant. Once the grant is in place, "Choose apps now" finishes setup and opens
 * Payment capture directly, so picking the first banking app is one tap away
 * instead of a search through Settings. Continue stays enabled either way:
 * capture is never a gate, and the allowlist lives in Settings because a rule
 * binds an app to an account that only exists after the wizard seeds them.
 */
@Composable
fun FirstRunScreen(
    access: NotificationAccess = rememberNotificationAccess(),
    onComplete: (AppLanguage, Currency, ThemeMode, openCaptureSettings: Boolean) -> Unit,
) {
    var language by rememberSaveable {
        mutableStateOf(AppLanguage.resolve(AppLanguage.SYSTEM, platformLocaleTag()))
    }
    var currency by rememberSaveable { mutableStateOf(Currency.CHF) }
    var theme by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }

    val steps = firstRunSteps(access.supported)
    val step = steps[stepIndex]

    ApplyLanguageOverride(language)
    key(language) {
        LindenTheme(themeMode = theme) {
            Column(
                modifier = Modifier
                    .screenContainer()
                    .testTag("firstRunScreen"),
            ) {
                if (step != FirstRunStep.Welcome) {
                    if (stepIndex > 0) {
                        ScreenHeader(
                            title = stepTitle(step),
                            onBack = { stepIndex-- },
                        )
                    } else {
                        Text(
                            text = stepTitle(step),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag("screenTitle"),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (step) {
                        FirstRunStep.Welcome -> FirstRunWelcome()

                        FirstRunStep.Language -> LanguageStep(language) { language = it }

                        FirstRunStep.Currency -> CurrencyStep(currency) { currency = it }

                        FirstRunStep.Theme -> ThemeStep(theme) { theme = it }

                        FirstRunStep.Capture -> CaptureStep(
                            access = access,
                            onChooseApps = { onComplete(language, currency, theme, true) },
                        )
                    }
                }

                FirstRunFooter(
                    canGoBack = stepIndex > 0,
                    isWelcome = step == FirstRunStep.Welcome,
                    onBack = { stepIndex-- },
                    onContinue = {
                        if (stepIndex == steps.lastIndex) {
                            onComplete(language, currency, theme, false)
                        } else {
                            stepIndex++
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun stepTitle(step: FirstRunStep): String = when (step) {
    FirstRunStep.Language -> stringResource(Res.string.first_run_choose_language)
    FirstRunStep.Currency -> stringResource(Res.string.first_run_choose_currency)
    FirstRunStep.Theme -> stringResource(Res.string.first_run_choose_theme)
    FirstRunStep.Capture -> stringResource(Res.string.capture_title)
    FirstRunStep.Welcome -> ""
}

@Composable
private fun LanguageStep(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        languageOptions.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option.label()) },
                modifier = Modifier.testTag("firstRunLanguage-${option.name}"),
            )
        }
    }
}

@Composable
private fun CurrencyStep(selected: Currency, onSelect: (Currency) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Currency.entries.forEach { entry ->
            FilterChip(
                selected = selected == entry,
                onClick = { onSelect(entry) },
                label = { Text(entry.name) },
                modifier = Modifier.testTag("firstRunCurrency-${entry.name}"),
            )
        }
    }
}

@Composable
private fun ThemeStep(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    ThemeModePicker(
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth(),
        tagPrefix = "firstRunTheme-",
    )
}

@Composable
private fun CaptureStep(access: NotificationAccess, onChooseApps: () -> Unit) {
    Text(
        text = stringResource(Res.string.capture_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = stringResource(Res.string.first_run_capture_how),
        style = MaterialTheme.typography.titleSmall,
    )
    Spacer(modifier = Modifier.height(4.dp))
    CaptureStepRow(number = 1, text = stringResource(Res.string.first_run_capture_step_access))
    CaptureStepRow(number = 2, text = stringResource(Res.string.first_run_capture_step_finish))
    CaptureStepRow(number = 3, text = stringResource(Res.string.first_run_capture_step_add))
    Spacer(modifier = Modifier.height(16.dp))
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
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(
            onClick = onChooseApps,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("firstRunCaptureChooseApps"),
        ) {
            Text(stringResource(Res.string.first_run_capture_choose_now))
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

@Composable
private fun CaptureStepRow(number: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(20.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FirstRunFooter(canGoBack: Boolean, isWelcome: Boolean, onBack: () -> Unit, onContinue: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canGoBack) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.testTag("firstRunBack"),
            ) {
                Text(stringResource(Res.string.common_back))
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Button(
            onClick = onContinue,
            modifier = Modifier
                .weight(1f)
                .testTag("firstRunContinue"),
        ) {
            Text(
                stringResource(
                    if (isWelcome) Res.string.first_run_get_started else Res.string.first_run_continue,
                ),
            )
        }
    }
}
