package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_back
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ErrorSnackbar
import org.sjbtimdan.linden.ui.LocalBottomBarContentPadding
import org.sjbtimdan.linden.ui.SlidingPillTab
import org.sjbtimdan.linden.ui.SlidingPillTabs
import org.sjbtimdan.linden.ui.capture.CapturedPaymentsBanner
import org.sjbtimdan.linden.ui.capture.CapturedPaymentsViewModel
import org.sjbtimdan.linden.ui.rates.RatesWarning
import org.sjbtimdan.linden.ui.screenContainerWithIme

private val entryTypes = listOf(EntryType.Expense, EntryType.Income, EntryType.Transfer)

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

    // The type tabs are static per language: resolve the three labels once and
    // remember the tab list keyed on them, so a keystroke in the form doesn't
    // rebuild the list (and force SlidingPillTabs to recompose). A language
    // change discards this via App's key(language) recomposition.
    val expenseName = EntryType.Expense.displayName()
    val incomeName = EntryType.Income.displayName()
    val transferName = EntryType.Transfer.displayName()
    val entryTabs = remember(expenseName, incomeName, transferName) {
        listOf(
            SlidingPillTab(expenseName, EntryType.Expense.icon(), "entryType-Expense"),
            SlidingPillTab(incomeName, EntryType.Income.icon(), "entryType-Income"),
            SlidingPillTab(transferName, EntryType.Transfer.icon(), "entryType-Transfer"),
        )
    }

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
                tabs = entryTabs,
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

        if (!fieldFocused) {
            Spacer(modifier = Modifier.height(8.dp))

            SnackbarHost(hostState = snackbarHostState)

            Spacer(modifier = Modifier.height(8.dp))

            MissingRequirementRegion(
                draft = draft,
                accounts = accounts,
                categories = categories,
                draftEdited = draftEdited,
                dialogs = formDialogs,
                onDialogsChange = onDialogsChange,
                defaultCurrency = defaultCurrency,
            )

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

            EntryActionsRow(
                draft = draft,
                addEnabled = draft?.isValid(accounts) == true,
                onAdd = addDraft,
                onClear = {
                    viewModel.clearDraft()
                    draftTouched = false
                    draftEdited = false
                },
            )
        }
    }
}
