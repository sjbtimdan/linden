package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_back
import org.sjbtimdan.linden.resources.common_clear
import org.sjbtimdan.linden.resources.entry_add
import org.sjbtimdan.linden.resources.entry_hide_total
import org.sjbtimdan.linden.resources.entry_show_total
import org.sjbtimdan.linden.resources.entry_total_balance
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ErrorSnackbar
import org.sjbtimdan.linden.ui.accounts.AccountDialogState
import org.sjbtimdan.linden.ui.categories.CategoryDialogState
import org.sjbtimdan.linden.ui.rates.RatesWarning
import org.sjbtimdan.linden.ui.rates.RatesWarningBanner
import org.sjbtimdan.linden.ui.screenContainerWithIme
import org.sjbtimdan.linden.ui.theme.CardElevation

private val entryTypes = listOf(EntryType.Expense, EntryType.Income, EntryType.Transfer)

@Composable
fun EntryPoint(
    viewModel: EntryPointViewModel,
    onNavigateToRates: () -> Unit = {},
    ratesWarning: RatesWarning? = null,
    // Test seam: desktop has no system back, so the real BackHandler is a no-op
    // and tests inject a handler they can invoke.
    systemBackHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit = { enabled, onBack ->
        BackHandler(enabled, onBack)
    },
) {
    // Only visible accounts can back a new entry: pickers, suggestions and the
    // seed all exclude hidden ones, so the form never offers an account to hide.
    val accounts by viewModel.visibleAccounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val descriptionSuggestions by viewModel.descriptionSuggestions.collectAsState()
    val accountSuggestions by viewModel.accountSuggestions.collectAsState()
    val categorySuggestions by viewModel.categorySuggestions.collectAsState()
    val quickEntries by viewModel.quickEntries.collectAsState()
    val totalMinor by viewModel.totalMinor.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val hideTotal by viewModel.hideTotal.collectAsState()
    val hasEntries by viewModel.hasEntries.collectAsState()
    val showRatesWarning by viewModel.showRatesWarning.collectAsState()
    val lastAdded by viewModel.lastAdded.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Seeds the draft from the last entry of the selected type. The ViewModel
    // keeps the draft across configuration changes, so this runs only when the
    // screen (and thus the draft) is created fresh.
    LaunchedEffect(Unit) {
        viewModel.seedDraft()
    }

    viewModel.ErrorSnackbar(snackbarHostState)

    var fieldFocused by remember { mutableStateOf(false) }

    // Once the user starts a draft the hero card and the rates banner shrink to
    // slim rows so the form keeps the viewport; Clear restores them. A fresh
    // prefill from the last entry does not count as a touch. Saveable so a
    // draft that survives a configuration change keeps the compact header.
    var draftTouched by rememberSaveable { mutableStateOf(false) }
    val markTouched: () -> Unit = { draftTouched = true }

    // Saves the draft from either the form's Add row or the calculator's Add
    // button; both only fire while the draft is valid. The persistent last-added
    // receipt is the confirmation, so no snackbar is shown.
    val addDraft: () -> Unit = {
        scope.launch {
            if (viewModel.saveDraft()) markTouched()
        }
    }

    // Create dialogs shared with EntryForm and opened from the missing-requirement
    // hint when no accounts/categories exist. EntryForm renders them from this state.
    var createCategoryDialog by remember { mutableStateOf<CategoryDialogState?>(null) }
    var createAccountDialog by remember { mutableStateOf<AccountDialogState?>(null) }

    // The back arrow and the system back both exit editing: clearFocus closes
    // text fields and dropdowns, bumping editEpoch closes EntryForm's
    // calculators. Neither clears the draft — a half-finished entry survives a
    // back press, and Clear is the full reset.
    var editEpoch by remember { mutableStateOf(0) }
    val exitEditing: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        editEpoch++
    }

    systemBackHandler(draft != null, exitEditing)

    Column(
        modifier = Modifier
            .screenContainerWithIme()
            // Tapping outside a field hides the keyboard too — focus loss alone
            // doesn't always close the IME on Android.
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            },
    ) {
        if (!fieldFocused) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                entryTypes.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { viewModel.selectType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = entryTypes.size),
                        icon = {
                            Icon(
                                imageVector = type.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        label = { Text(type.displayName()) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The hero card is dead weight before the first entry exists.
            if (hasEntries) {
                TotalBalanceCard(
                    total = totalMinor,
                    currency = defaultCurrency,
                    hidden = hideTotal,
                    compact = draftTouched,
                    onToggleHidden = { viewModel.setHideTotal(!hideTotal) },
                )
            }

            // FX rates only matter once entries span currencies or the user has
            // visited the Rates screen; until then the warning is day-one noise.
            if (showRatesWarning) {
                ratesWarning?.let { warning ->
                    Spacer(modifier = Modifier.height(12.dp))

                    RatesWarningBanner(
                        warning = warning,
                        onSetRates = onNavigateToRates,
                        compact = draftTouched,
                    )
                }
            }
        }
        if (fieldFocused) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = exitEditing) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.common_back),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            draft?.let { state ->
                EntryForm(
                    state = state,
                    accounts = accounts,
                    categories = categories,
                    onAmountChange = {
                        viewModel.onAmountChange(it)
                        markTouched()
                    },
                    onCategoryChange = {
                        viewModel.onCategoryChange(it)
                        markTouched()
                    },
                    onAccountChange = {
                        viewModel.onAccountChange(it)
                        markTouched()
                    },
                    onToAccountChange = {
                        viewModel.onToAccountChange(it)
                        markTouched()
                    },
                    onToAmountChange = {
                        viewModel.onToAmountChange(it)
                        markTouched()
                    },
                    onDescriptionChange = {
                        viewModel.onDescriptionChange(it)
                        markTouched()
                    },
                    onCreatedAtChange = {
                        viewModel.onCreatedAtChange(it)
                        markTouched()
                    },
                    onFieldFocusChange = { fieldFocused = it },
                    editEpoch = editEpoch,
                    descriptionSuggestions = descriptionSuggestions,
                    accountSuggestions = accountSuggestions,
                    categorySuggestions = categorySuggestions,
                    quickEntries = quickEntries,
                    onQuickEntry = {
                        viewModel.applyQuickEntry(it)
                        markTouched()
                    },
                    onAdd = addDraft,
                    defaultCurrency = defaultCurrency,
                    onCreateCategory = { name, type, icon ->
                        val created = viewModel.createCategory(name, type, icon)
                        if (created) markTouched()
                        created
                    },
                    onCreateAccount = { name, currency, initialBalance, selectAsTo ->
                        val created = viewModel.createAccount(name, currency, initialBalance, selectAsTo)
                        if (created) markTouched()
                        created
                    },
                    categoryDialogState = createCategoryDialog,
                    onCategoryDialogStateChange = { createCategoryDialog = it },
                    accountDialogState = createAccountDialog,
                    onAccountDialogStateChange = { createAccountDialog = it },
                )
            }
        }

        // Explains why Add is disabled unless the form's own links already
        // point at the blocker (missing accounts or categories).
        val missingHint = missingRequirement(draft, accounts, categories)

        // When nothing exists to pick, the hint becomes an action that opens the
        // same create dialogs as the form's "+ New" chips.
        val openCreateCategoryFromHint: () -> Unit = {
            draft?.type?.let { type ->
                createCategoryDialog = CategoryDialogState(
                    category = null,
                    name = "",
                    type = type.toCategoryType(),
                )
            }
        }
        val openCreateAccountFromHint: () -> Unit = {
            createAccountDialog = AccountDialogState(
                account = null,
                name = "",
                currency = defaultCurrency,
                initialBalanceText = "",
                selectAsTo = false,
            )
        }

        if (!fieldFocused) {
            Spacer(modifier = Modifier.height(8.dp))

            SnackbarHost(hostState = snackbarHostState)

            Spacer(modifier = Modifier.height(8.dp))

            missingHint?.let { hint ->
                MissingRequirementHint(
                    message = hint.text(),
                    onClick = when (hint) {
                        MissingRequirement.NO_ACCOUNTS -> openCreateAccountFromHint
                        MissingRequirement.NO_CATEGORY -> openCreateCategoryFromHint
                        else -> null
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Receipt of the entry just saved, clearly marked as added so the
            // prefilled form below is not mistaken for an unsaved draft. Tapping
            // it pulls the entry back into the form as an undo.
            lastAdded?.let { entry ->
                LastAddedEntry(
                    entry = entry,
                    hideAmounts = hideTotal,
                    onClick = {
                        viewModel.undoLastAdded()
                        markTouched()
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = addDraft,
                    enabled = draft?.isValid(accounts) == true,
                    modifier = Modifier.weight(1f).testTag("saveEntry"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(Res.string.entry_add))
                }

                OutlinedButton(
                    onClick = {
                        viewModel.clearDraft()
                        draftTouched = false
                    },
                    enabled = draft != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.common_clear))
                }
            }
        }
    }
}

/** Total across all accounts in the default currency; null while a rate is missing. */
@Composable
private fun TotalBalanceCard(
    total: Long?,
    currency: Currency,
    hidden: Boolean,
    compact: Boolean,
    onToggleHidden: () -> Unit,
) {
    val amountLabel = if (hidden) HIDDEN_AMOUNT else total?.let(::formatAmountCompact) ?: "–"
    if (compact) {
        // Slim one-line variant shown while a draft is being captured.
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("totalBalanceCompact"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = CardElevation,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.entry_total_balance),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = amountLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currency.symbol,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                IconButton(onClick = onToggleHidden) {
                    Icon(
                        imageVector = if (hidden) VisibilityOffIcon else VisibilityIcon,
                        contentDescription = if (hidden) {
                            stringResource(Res.string.entry_show_total)
                        } else {
                            stringResource(Res.string.entry_hide_total)
                        },
                    )
                }
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("totalBalanceCard"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = CardElevation,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.entry_total_balance),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onToggleHidden) {
                        Icon(
                            imageVector = if (hidden) VisibilityOffIcon else VisibilityIcon,
                            contentDescription = if (hidden) {
                                stringResource(Res.string.entry_show_total)
                            } else {
                                stringResource(Res.string.entry_hide_total)
                            },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = amountLabel,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currency.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
        }
    }
}
