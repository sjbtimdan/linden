package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
import org.sjbtimdan.linden.resources.entry_amount
import org.sjbtimdan.linden.resources.entry_amount_positive
import org.sjbtimdan.linden.resources.entry_amount_received
import org.sjbtimdan.linden.resources.entry_amount_sent
import org.sjbtimdan.linden.resources.entry_new_account
import org.sjbtimdan.linden.resources.entry_new_account_with_name
import org.sjbtimdan.linden.resources.entry_new_category
import org.sjbtimdan.linden.resources.entry_new_category_with_name
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.accounts.AccountDialogHost
import org.sjbtimdan.linden.ui.accounts.AccountDialogState
import org.sjbtimdan.linden.ui.categories.CategoryDialogHost
import org.sjbtimdan.linden.ui.categories.CategoryDialogState
import kotlin.time.Instant

/** Which field is expanded right now; while one is, the rest of the form collapses. */
internal enum class ActiveField { Description, From, To, Category, Account, Amount, ToAmount }

/** The category type a new category should get for an entry of [type]. */
internal fun EntryType.toCategoryType(): CategoryType = when (this) {
    EntryType.Expense -> CategoryType.Expense
    EntryType.Income -> CategoryType.Income
    EntryType.Transfer -> CategoryType.Both
}

/**
 * The field section of the entry editor, shared between the inline form on the
 * entry screen and the edit dialog on the ledger screen.
 */
@Composable
fun EntryForm(
    state: EntryDraft,
    accounts: List<Account>,
    categories: List<Category>,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onAccountChange: (Long?) -> Unit,
    onToAccountChange: (Long?) -> Unit,
    onToAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCreatedAtChange: (Instant) -> Unit,
    onFieldFocusChange: (Boolean) -> Unit = {},
    onCalculatorOpenChange: (Boolean) -> Unit = {},
    editEpoch: Int = 0,
    suggestions: EntryFormSuggestions = EmptyEntryFormSuggestions,
    onQuickEntry: (QuickEntry) -> Unit = {},
    onUpcoming: (UpcomingRecurring) -> Unit = {},
    onDismissUpcoming: (UpcomingRecurring) -> Unit = {},
    dismissedUpcoming: String? = null,
    onUndoDismissUpcoming: () -> Unit = {},
    onClearDismissUpcoming: () -> Unit = {},
    defaultCurrency: Currency = Currency.CHF,
    paceCaption: (@Composable () -> Unit)? = null,
    onCreateCategory: ((String, CategoryType, CategoryIcon?) -> Boolean)? = null,
    onCreateAccount: ((String, Currency, Long, Boolean) -> Boolean)? = null,
    // Create-dialog state hoisted by a host that wants to open the same dialogs
    // from outside the form (e.g. from its missing-requirement hint). When the
    // holder is left null, EntryForm remembers its own dialog state.
    dialogs: EntryFormDialogs? = null,
    onDialogsChange: ((EntryFormDialogs) -> Unit)? = null,
) {
    val ui = rememberEntryFormUi(state, accounts, categories, suggestions)

    // Localized copy for this form; resolved once per composition so plain
    // lambdas (onInvalid, dropdown "missing" texts) can capture the strings.
    val amountLabel = if (state.type == EntryType.Transfer) {
        stringResource(Res.string.entry_amount_sent)
    } else {
        stringResource(Res.string.entry_amount)
    }
    val receivedAmountLabel = stringResource(Res.string.entry_amount_received)
    val amountPositiveWarning = stringResource(Res.string.entry_amount_positive)
    val newCategoryText = stringResource(Res.string.entry_new_category)
    val newCategoryWithName = stringResource(Res.string.entry_new_category_with_name)
    val newAccountText = stringResource(Res.string.entry_new_account)
    val newAccountWithName = stringResource(Res.string.entry_new_account_with_name)
    val accountCreateLabel: ((String) -> String)? = if (onCreateAccount != null) {
        { query -> if (query.isBlank()) newAccountText else newAccountWithName.format(query) }
    } else {
        null
    }
    val categoryCreateLabel: ((String) -> String)? = if (onCreateCategory != null) {
        { query -> if (query.isBlank()) newCategoryText else newCategoryWithName.format(query) }
    } else {
        null
    }

    // Create dialogs, opened from the "+ New" chips or from hoisted state a
    // host seeded via the missing-requirement hint. The host's state wins when
    // provided; otherwise the dialogs are remembered inside the form.
    var internalDialogs by remember { mutableStateOf(EntryFormDialogs()) }
    val currentDialogs = dialogs ?: internalDialogs
    val setDialogs: (EntryFormDialogs) -> Unit = onDialogsChange ?: { internalDialogs = it }
    val updateDialogs: ((EntryFormDialogs) -> EntryFormDialogs) -> Unit = { transform ->
        setDialogs(transform(currentDialogs))
    }
    val openCreateCategory: (String) -> Unit = { query ->
        updateDialogs {
            it.copy(
                category = CategoryDialogState(
                    category = null,
                    name = query,
                    type = state.type.toCategoryType(),
                ),
            )
        }
    }
    val openCreateAccount: (String, Boolean) -> Unit = { query, selectAsTo ->
        updateDialogs {
            it.copy(
                account = AccountDialogState(
                    account = null,
                    name = query,
                    currency = defaultCurrency,
                    initialBalanceText = "",
                    selectAsTo = selectAsTo,
                ),
            )
        }
    }

    // While a field is focused the form collapses so that field and its options
    // get the whole area above the keyboard. The focused field must stay mounted
    // (unmounting drops focus), so sections hide only while another is active.
    var activeField by remember { mutableStateOf<ActiveField?>(null) }
    var amountWarning by remember { mutableStateOf<String?>(null) }
    var toAmountWarning by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeField) {
        onFieldFocusChange(activeField != null)
        onCalculatorOpenChange(activeField == ActiveField.Amount || activeField == ActiveField.ToAmount)
    }
    val editing = activeField != null

    LaunchedEffect(editEpoch) {
        if (activeField == ActiveField.Amount || activeField == ActiveField.ToAmount) activeField = null
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun openCalculator() {
        activeField = ActiveField.Amount
        amountWarning = null
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    // The back key closes an expanded dropdown, calculator, or description field.
    BackHandler(enabled = activeField != null) {
        activeField = null
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (!editing) {
            dismissedUpcoming?.let { description ->
                UpcomingDismissBar(
                    description = description,
                    onUndo = onUndoDismissUpcoming,
                    onDismiss = onClearDismissUpcoming,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (suggestions.upcomingRecurring.isNotEmpty()) {
                UpcomingRecurringSection(
                    upcoming = suggestions.upcomingRecurring,
                    zone = state.createdZone,
                    onSelect = onUpcoming,
                    onDismiss = onDismissUpcoming,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            AmountField(
                value = state.amountText,
                label = amountLabel,
                suffix = ui.fromAccount?.currency?.symbol,
                warning = amountWarning,
                onValueChange = {
                    onAmountChange(it)
                    amountWarning = null
                },
                onFocus = ::openCalculator,
                modifier = Modifier.testTag("amountField"),
            )
            paceCaption?.let { caption ->
                Spacer(modifier = Modifier.height(4.dp))
                caption()
            }
        }

        EntrySelectFields(
            ui = ui,
            state = state,
            activeField = activeField,
            toAmountWarning = toAmountWarning,
            accountCreateLabel = accountCreateLabel,
            categoryCreateLabel = categoryCreateLabel,
            onAccountChange = onAccountChange,
            onToAccountChange = onToAccountChange,
            onCategoryChange = onCategoryChange,
            onToAmountChange = onToAmountChange,
            onActiveFieldChange = { activeField = it },
            onOpenCreateAccount = { query, selectAsTo -> openCreateAccount(query, selectAsTo) },
            onOpenCreateCategory = { query -> openCreateCategory(query) },
            onToAmountWarningClear = { toAmountWarning = null },
        )

        if (!editing) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (activeField == null || activeField == ActiveField.Description) {
            EntryDescriptionField(
                value = state.description,
                suggestions = suggestions.descriptions,
                expanded = activeField == ActiveField.Description,
                onValueChange = onDescriptionChange,
                onFocusChange = { focused ->
                    activeField = if (focused) ActiveField.Description else null
                },
            )
        }

        if (!editing) {
            Spacer(modifier = Modifier.height(16.dp))
            DateAndTimeButtons(
                createdAt = state.createdAt,
                createdZone = state.createdZone,
                onChange = onCreatedAtChange,
            )

            if (suggestions.quickEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                EntryQuickEntrySection(
                    quickEntries = suggestions.quickEntries,
                    onSelect = onQuickEntry,
                )
            }
        }
    }

    EntryAmountCalculators(
        activeField = activeField,
        state = state,
        fromAccount = ui.fromAccount,
        toAccount = ui.toAccount,
        amountLabel = amountLabel,
        receivedAmountLabel = receivedAmountLabel,
        amountPositiveWarning = amountPositiveWarning,
        onAmountChange = onAmountChange,
        onToAmountChange = onToAmountChange,
        onAmountWarning = { amountWarning = it },
        onToAmountWarning = { toAmountWarning = it },
        onCancel = { activeField = null },
    )

    CategoryDialogHost(
        state = currentDialogs.category,
        isEditing = false,
        canDelete = false,
        onStateChange = { category -> updateDialogs { it.copy(category = category) } },
        onSave = { name, type, icon -> onCreateCategory?.invoke(name, type, icon) ?: false },
    )

    AccountDialogHost(
        state = currentDialogs.account,
        isEditing = false,
        canChangeCurrency = true,
        canDelete = false,
        onStateChange = { account -> updateDialogs { it.copy(account = account) } },
        onSave = { name, currency, initialBalance, selectAsTo ->
            onCreateAccount?.invoke(name, currency, initialBalance, selectAsTo) ?: false
        },
    )
}
