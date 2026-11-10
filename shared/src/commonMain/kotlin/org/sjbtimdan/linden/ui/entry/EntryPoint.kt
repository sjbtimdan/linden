package org.sjbtimdan.linden.ui.entry

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.predictions.QuickEntry
import org.sjbtimdan.linden.predictions.UpcomingRecurring
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_back
import org.sjbtimdan.linden.resources.common_clear
import org.sjbtimdan.linden.resources.common_save
import org.sjbtimdan.linden.resources.entry_add
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ErrorSnackbar
import org.sjbtimdan.linden.ui.LocalBottomBarContentPadding
import org.sjbtimdan.linden.ui.SlidingPillTab
import org.sjbtimdan.linden.ui.SlidingPillTabs
import org.sjbtimdan.linden.ui.accounts.AccountDialogState
import org.sjbtimdan.linden.ui.capture.CapturedPaymentsBanner
import org.sjbtimdan.linden.ui.capture.CapturedPaymentsViewModel
import org.sjbtimdan.linden.ui.categories.CategoryDialogState
import org.sjbtimdan.linden.ui.rates.RatesWarning
import org.sjbtimdan.linden.ui.rates.RatesWarningBanner
import org.sjbtimdan.linden.ui.screenContainerWithIme
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val entryTypes = listOf(EntryType.Expense, EntryType.Income, EntryType.Transfer)

/**
 * How long the last-added receipt stays as an undo offer before it dismisses
 * itself; after that the add is final.
 */
private val LAST_ADDED_UNDO_WINDOW = 5.seconds

@Composable
fun EntryPoint(
    viewModel: EntryPointViewModel,
    onNavigateToRates: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    ratesWarning: RatesWarning? = null,
    captureViewModel: CapturedPaymentsViewModel? = null,
    // Test seam: desktop has no system back, so the real BackHandler is a no-op
    // and tests inject a handler they can invoke.
    systemBackHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit = { enabled, onBack ->
        BackHandler(enabled, onBack)
    },
) {
    // Only visible accounts can back a new entry: pickers, suggestions and the
    // seed all exclude hidden ones, so the form never offers an account to hide.
    // The screen root keeps only the flows its chrome needs; the hero card,
    // banners and the form collect their own below so a change in one subtree
    // does not recompose the whole screen.
    val accounts by viewModel.visibleAccounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
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
    val markTouched: () -> Unit = remember { { draftTouched = true } }

    // Whether the user has edited the form since the last save or clear. Opening
    // the screen, a prefill from the last entry, and the reset after a save are
    // not user edits, so the missing-requirement hint stays hidden until one.
    var draftEdited by rememberSaveable { mutableStateOf(false) }
    val markEdited: () -> Unit = remember {
        {
            markTouched()
            draftEdited = true
        }
    }

    // A draft loaded from a captured payment arrives prefilled but incomplete —
    // the category is always the user's to pick — so treat the load as an edit
    // and let the missing-field hint explain the disabled Add right away. It is
    // also a started draft, so the header compacts to keep the form in view.
    LaunchedEffect(draft?.sourceCaptureId) {
        if (draft?.sourceCaptureId != null) {
            markTouched()
            markEdited()
        }
    }

    // Saves the draft from either the form's Add row or the calculator's Add
    // button; both only fire while the draft is valid. The persistent last-added
    // receipt is the confirmation, so no snackbar is shown. The capture inbox
    // is refreshed explicitly afterwards: a capture written by the listener's
    // own connection is invisible to the app's flows until re-read.
    val addDraft: () -> Unit = {
        scope.launch {
            if (viewModel.saveDraft()) {
                markTouched()
                draftEdited = false
                captureViewModel?.refresh()
            }
        }
    }

    // Create dialogs shared with EntryForm and opened from the missing-requirement
    // hint when no accounts/categories exist. EntryForm renders them from this state.
    var formDialogs by remember { mutableStateOf(EntryFormDialogs()) }
    // Stable callbacks for the form, so EntryForm can skip recomposition when
    // its inputs are unchanged.
    val onFieldFocusChange = remember { { focused: Boolean -> fieldFocused = focused } }
    val onDialogsChange = remember { { dialogs: EntryFormDialogs -> formDialogs = dialogs } }

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
            .padding(bottom = LocalBottomBarContentPadding.current)
            // Tapping outside a field hides the keyboard too — focus loss alone
            // doesn't always close the IME on Android.
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            },
    ) {
        // The captured-payment inbox lives where its entries will: pinned to the
        // top of the entry screen, collapsed to a bar once acknowledged.
        if (captureViewModel != null) {
            CapturedPaymentsBanner(
                viewModel = captureViewModel,
                onAdd = { capture ->
                    viewModel.loadDraft(captureViewModel.buildDraft(capture))
                },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        if (!fieldFocused) {
            SlidingPillTabs(
                tabs = entryTypes.map { type ->
                    SlidingPillTab(
                        label = type.displayName(),
                        icon = type.icon(),
                        tag = "entryType-${type.name}",
                    )
                },
                selectedIndex = entryTypes.indexOf(selectedType).coerceAtLeast(0),
                onSelect = { index -> viewModel.selectType(entryTypes[index]) },
            )

            Spacer(modifier = Modifier.height(12.dp))

            EntryHeaderRegion(
                viewModel = viewModel,
                ratesWarning = ratesWarning,
                compact = draftTouched,
                onSetRates = onNavigateToRates,
                onNavigateToInsights = onNavigateToInsights,
            )
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
                EntryFormRegion(
                    viewModel = viewModel,
                    state = state,
                    accounts = accounts,
                    categories = categories,
                    defaultCurrency = defaultCurrency,
                    editEpoch = editEpoch,
                    onFieldFocusChange = onFieldFocusChange,
                    markEdited = markEdited,
                    dialogs = formDialogs,
                    onDialogsChange = onDialogsChange,
                )
            }
        }

        // Explains why Add is disabled, but only once the user has edited the
        // form: a fresh screen or the reset after a save is not an error. The
        // blockers that cannot be picked around (nothing to create an entry
        // with) still show immediately as their fix is to create one.
        val missingHint = missingRequirement(draft, accounts, categories)
            ?.takeIf { draftEdited || it.isCreateAction }

        // When nothing exists to pick, the hint becomes an action that opens the
        // same create dialogs as the form's "+ New" chips.
        val openCreateCategoryFromHint: () -> Unit = {
            draft?.type?.let { type ->
                formDialogs = formDialogs.copy(
                    category = CategoryDialogState(
                        category = null,
                        name = "",
                        type = type.toCategoryType(),
                    ),
                )
            }
        }
        val openCreateAccountFromHint: () -> Unit = {
            formDialogs = formDialogs.copy(
                account = AccountDialogState(
                    account = null,
                    name = "",
                    currency = defaultCurrency,
                    initialBalanceText = "",
                    selectAsTo = false,
                ),
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
            // prefilled form below is not mistaken for an unsaved draft. Its
            // Edit action pulls the entry back into the form to update in
            // place and Undo pulls it out of the ledger as a fresh draft, while
            // its leading icon dismisses the receipt; a tap on the body does
            // nothing.
            LastAddedRegion(
                viewModel = viewModel,
                onTouched = markTouched,
            )

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
                        imageVector = if (draft?.editing != null) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(
                            if (draft?.editing != null) Res.string.common_save else Res.string.entry_add,
                        ),
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.clearDraft()
                        draftTouched = false
                        draftEdited = false
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

@Composable
private fun EntryHeaderRegion(
    viewModel: EntryPointViewModel,
    ratesWarning: RatesWarning?,
    compact: Boolean,
    onSetRates: () -> Unit,
    onNavigateToInsights: () -> Unit,
) {
    val hasEntries by viewModel.hasEntries.collectAsState()
    val totalMinor by viewModel.totalMinor.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val hideTotal by viewModel.hideTotal.collectAsState()
    val showRatesWarning by viewModel.showRatesWarning.collectAsState()

    // The hero card is dead weight before the first entry exists.
    if (hasEntries) {
        TotalBalanceCard(
            total = totalMinor,
            currency = defaultCurrency,
            hidden = hideTotal,
            compact = compact,
            onToggleHidden = { viewModel.setHideTotal(!hideTotal) },
            onNavigateToInsights = onNavigateToInsights,
        )
    }

    // FX rates only matter once entries span currencies or the user has
    // visited the Rates screen; until then the warning is day-one noise.
    if (showRatesWarning) {
        ratesWarning?.let { warning ->
            Spacer(modifier = Modifier.height(12.dp))

            RatesWarningBanner(
                warning = warning,
                onSetRates = onSetRates,
                compact = compact,
            )
        }
    }
}

@Composable
private fun LastAddedRegion(viewModel: EntryPointViewModel, onTouched: () -> Unit) {
    val lastAdded by viewModel.lastAdded.collectAsState()
    val lastAddedWasEdit by viewModel.lastAddedWasEdit.collectAsState()
    val hideTotal by viewModel.hideTotal.collectAsState()

    // The receipt dismisses itself after a short window, after which the add is final.
    LaunchedEffect(lastAdded?.id) {
        if (lastAdded != null) {
            delay(LAST_ADDED_UNDO_WINDOW)
            viewModel.dismissLastAdded()
        }
    }

    lastAdded?.let { entry ->
        LastAddedEntry(
            entry = entry,
            hideAmounts = hideTotal,
            updated = lastAddedWasEdit,
            onEdit = {
                viewModel.editLastAdded()
                onTouched()
            },
            onUndo = {
                viewModel.undoLastAdded()
                onTouched()
            },
            onDismiss = viewModel::dismissLastAdded,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun EntryFormRegion(
    viewModel: EntryPointViewModel,
    state: EntryDraft,
    accounts: List<Account>,
    categories: List<Category>,
    defaultCurrency: Currency,
    editEpoch: Int,
    onFieldFocusChange: (Boolean) -> Unit,
    markEdited: () -> Unit,
    dialogs: EntryFormDialogs,
    onDialogsChange: (EntryFormDialogs) -> Unit,
) {
    val descriptionSuggestions by viewModel.descriptionSuggestions.collectAsState()
    val accountSuggestions by viewModel.accountSuggestions.collectAsState()
    val categorySuggestions by viewModel.categorySuggestions.collectAsState()
    val quickEntries by viewModel.quickEntries.collectAsState()
    val upcomingRecurring by viewModel.upcomingRecurring.collectAsState()
    val lastDismissed by viewModel.lastDismissed.collectAsState()

    // The dismiss undo offer expires after a short window, like the added-entry receipt.
    LaunchedEffect(lastDismissed) {
        if (lastDismissed != null) {
            delay(LAST_ADDED_UNDO_WINDOW)
            viewModel.clearLastDismiss()
        }
    }
    val paceAfterThis by viewModel.paceAfterThis.collectAsState()
    val hideTotal by viewModel.hideTotal.collectAsState()
    val paceCaption: (@Composable () -> Unit)? = remember(hideTotal, paceAfterThis) {
        if (!hideTotal) {
            paceAfterThis?.let { pace -> { PaceCaption(pace = pace) } }
        } else {
            null
        }
    }

    // Remembered so the form can skip: a freshly allocated lambda on every
    // recomposition would make every callback param unstable.
    val onAmountChange = remember(viewModel, markEdited) {
        { text: String ->
            viewModel.onAmountChange(text)
            markEdited()
        }
    }
    val onCategoryChange = remember(viewModel, markEdited) {
        { id: Long? ->
            viewModel.onCategoryChange(id)
            markEdited()
        }
    }
    val onAccountChange = remember(viewModel, markEdited) {
        { id: Long? ->
            viewModel.onAccountChangeWithAutoCategory(id)
            markEdited()
        }
    }
    val onToAccountChange = remember(viewModel, markEdited) {
        { id: Long? ->
            viewModel.onToAccountChange(id)
            markEdited()
        }
    }
    val onToAmountChange = remember(viewModel, markEdited) {
        { text: String ->
            viewModel.onToAmountChange(text)
            markEdited()
        }
    }
    val onDescriptionChange = remember(viewModel, markEdited) {
        { text: String ->
            viewModel.onDescriptionChange(text)
            markEdited()
        }
    }
    val onCreatedAtChange = remember(viewModel, markEdited) {
        { instant: Instant ->
            viewModel.onCreatedAtChange(instant)
            markEdited()
        }
    }
    val onQuickEntry = remember(viewModel, markEdited) {
        { entry: QuickEntry ->
            viewModel.applyQuickEntry(entry)
            markEdited()
        }
    }
    val onUpcoming = remember(viewModel, markEdited) {
        { upcoming: UpcomingRecurring ->
            viewModel.applyQuickEntry(QuickEntry(upcoming.entry, upcoming.cadence))
            markEdited()
        }
    }
    val onDismissUpcoming = remember(viewModel) {
        { upcoming: UpcomingRecurring ->
            viewModel.dismissUpcoming(upcoming)
        }
    }
    val onUndoDismissUpcoming = remember(viewModel) { { viewModel.undoLastDismiss() } }
    val onClearDismissUpcoming = remember(viewModel) { { viewModel.clearLastDismiss() } }
    val onCreateCategory = remember(viewModel, markEdited) {
        { name: String, type: CategoryType, icon: CategoryIcon? ->
            val created = viewModel.createCategory(name, type, icon)
            if (created) markEdited()
            created
        }
    }
    val onCreateAccount = remember(viewModel, markEdited) {
        { name: String, currency: Currency, initialBalance: Long, selectAsTo: Boolean ->
            val created = viewModel.createAccount(name, currency, initialBalance, selectAsTo)
            if (created) markEdited()
            created
        }
    }

    EntryForm(
        state = state,
        accounts = accounts,
        categories = categories,
        onAmountChange = onAmountChange,
        onCategoryChange = onCategoryChange,
        onAccountChange = onAccountChange,
        onToAccountChange = onToAccountChange,
        onToAmountChange = onToAmountChange,
        onDescriptionChange = onDescriptionChange,
        onCreatedAtChange = onCreatedAtChange,
        onFieldFocusChange = onFieldFocusChange,
        editEpoch = editEpoch,
        suggestions = EntryFormSuggestions(
            descriptions = descriptionSuggestions,
            accountIds = accountSuggestions,
            categoryIds = categorySuggestions,
            quickEntries = quickEntries,
            upcomingRecurring = upcomingRecurring,
        ),
        onQuickEntry = onQuickEntry,
        onUpcoming = onUpcoming,
        onDismissUpcoming = onDismissUpcoming,
        dismissedUpcoming = lastDismissed,
        onUndoDismissUpcoming = onUndoDismissUpcoming,
        onClearDismissUpcoming = onClearDismissUpcoming,
        defaultCurrency = defaultCurrency,
        paceCaption = paceCaption,
        onCreateCategory = onCreateCategory,
        onCreateAccount = onCreateAccount,
        dialogs = dialogs,
        onDialogsChange = onDialogsChange,
    )
}
