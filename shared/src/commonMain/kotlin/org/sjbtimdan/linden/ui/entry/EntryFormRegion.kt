package org.sjbtimdan.linden.ui.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.predictions.QuickEntry
import org.sjbtimdan.linden.predictions.UpcomingRecurring
import kotlin.time.Instant

@Composable
internal fun EntryFormRegion(
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
