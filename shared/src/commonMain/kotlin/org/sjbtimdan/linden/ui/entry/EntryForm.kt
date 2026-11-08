package org.sjbtimdan.linden.ui.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import org.sjbtimdan.linden.resources.common_category
import org.sjbtimdan.linden.resources.entry_account
import org.sjbtimdan.linden.resources.entry_amount
import org.sjbtimdan.linden.resources.entry_amount_positive
import org.sjbtimdan.linden.resources.entry_amount_received
import org.sjbtimdan.linden.resources.entry_amount_sent
import org.sjbtimdan.linden.resources.entry_description_optional
import org.sjbtimdan.linden.resources.entry_from_account
import org.sjbtimdan.linden.resources.entry_new_account
import org.sjbtimdan.linden.resources.entry_new_account_with_name
import org.sjbtimdan.linden.resources.entry_new_category
import org.sjbtimdan.linden.resources.entry_new_category_with_name
import org.sjbtimdan.linden.resources.entry_ok
import org.sjbtimdan.linden.resources.entry_quick_entry
import org.sjbtimdan.linden.resources.entry_recurring
import org.sjbtimdan.linden.resources.entry_to_account
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ClearIconButton
import org.sjbtimdan.linden.ui.accounts.AccountDialogHost
import org.sjbtimdan.linden.ui.accounts.AccountDialogState
import org.sjbtimdan.linden.ui.categories.CategoryDialogHost
import org.sjbtimdan.linden.ui.categories.CategoryDialogState
import kotlin.time.Instant

/** Which field is expanded right now; while one is, the rest of the form collapses. */
private enum class ActiveField { Description, From, To, Category, Account, Amount, ToAmount }

/** The category type a new category should get for an entry of [type]. */
internal fun EntryType.toCategoryType(): CategoryType = when (this) {
    EntryType.Expense -> CategoryType.Expense
    EntryType.Income -> CategoryType.Income
    EntryType.Transfer -> CategoryType.Both
}

/** Keeps the quick-entry chips from sprawling when a description is long. */
private val QuickEntryChipMaxWidth = 220.dp

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
    val visibleCategories = remember(categories, state.type) { categoriesForType(categories, state.type) }
    val fromAccount = remember(accounts, state.accountId) { accounts.firstOrNull { it.id == state.accountId } }
    val toAccount = remember(accounts, state.toAccountId) { accounts.firstOrNull { it.id == state.toAccountId } }
    val toAccountOptions = remember(accounts, state.accountId) { accounts.filter { it.id != state.accountId } }
    val selectedCategory = remember(visibleCategories, state.categoryId) {
        visibleCategories.firstOrNull { it.id == state.categoryId }
    }
    val predictedCategoryOptions = remember(suggestions.categoryIds, visibleCategories, state.categoryId) {
        suggestions.categoryIds
            .filterNot { it == state.categoryId }
            .mapNotNull { id -> visibleCategories.firstOrNull { it.id == id } }
    }
    val predictedAccountOptions = remember(suggestions.accountIds, accounts, state.accountId) {
        suggestions.accountIds
            .filterNot { it == state.accountId }
            .mapNotNull { id -> accounts.firstOrNull { it.id == id } }
    }
    val showReceivedAmount = state.type == EntryType.Transfer &&
        fromAccount != null && toAccount != null &&
        fromAccount.currency != toAccount.currency

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
    // Recurring quick-entry chips get a subtle tint and a screen-reader label:
    // the chip itself only shows the description and the repeat icon.
    val recurringChipColors = OptionChipColors(
        container = MaterialTheme.colorScheme.tertiaryContainer,
        content = MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val recurringChipDescription = stringResource(Res.string.entry_recurring)

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

    fun openToCalculator() {
        activeField = ActiveField.ToAmount
        toAmountWarning = null
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
                suffix = fromAccount?.currency?.symbol,
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

        if (state.type == EntryType.Transfer) {
            if (!editing || activeField == ActiveField.From) {
                Spacer(modifier = Modifier.height(16.dp))
                DropdownField(
                    label = stringResource(Res.string.entry_from_account),
                    selected = fromAccount,
                    options = accounts,
                    optionLabel = { it.name },
                    onSelect = { onAccountChange(it.id) },
                    onFocusChange = { activeField = if (it) ActiveField.From else null },
                    createLabel = accountCreateLabel,
                    onCreate = if (onCreateAccount != null) {
                        { query -> openCreateAccount(query, false) }
                    } else {
                        null
                    },
                )
            }
            if (!editing || activeField == ActiveField.To) {
                Spacer(modifier = Modifier.height(16.dp))
                DropdownField(
                    label = stringResource(Res.string.entry_to_account),
                    selected = toAccount,
                    options = toAccountOptions,
                    optionLabel = { it.name },
                    onSelect = { onToAccountChange(it.id) },
                    onFocusChange = { activeField = if (it) ActiveField.To else null },
                    createLabel = accountCreateLabel,
                    onCreate = if (onCreateAccount != null) {
                        { query -> openCreateAccount(query, true) }
                    } else {
                        null
                    },
                )
            }
            if (!editing && showReceivedAmount) {
                Spacer(modifier = Modifier.height(16.dp))
                AmountField(
                    value = state.toAmountText,
                    label = receivedAmountLabel,
                    suffix = toAccount.currency.symbol,
                    warning = toAmountWarning,
                    onValueChange = {
                        onToAmountChange(it)
                        toAmountWarning = null
                    },
                    onFocus = ::openToCalculator,
                )
            }
        } else {
            if (!editing || activeField == ActiveField.Category) {
                Spacer(modifier = Modifier.height(16.dp))
                DropdownField(
                    label = stringResource(Res.string.common_category),
                    selected = selectedCategory,
                    options = visibleCategories,
                    optionLabel = { it.name },
                    onSelect = { onCategoryChange(it.id) },
                    onFocusChange = { activeField = if (it) ActiveField.Category else null },
                    predictedOptions = predictedCategoryOptions,
                    optionIcon = { it.icon?.imageVector() },
                    createLabel = categoryCreateLabel,
                    onCreate = if (onCreateCategory != null) {
                        { query -> openCreateCategory(query) }
                    } else {
                        null
                    },
                )
            }
            if (!editing || activeField == ActiveField.Account) {
                Spacer(modifier = Modifier.height(16.dp))
                DropdownField(
                    label = stringResource(Res.string.entry_account),
                    selected = fromAccount,
                    options = accounts,
                    optionLabel = { it.name },
                    onSelect = { onAccountChange(it.id) },
                    onFocusChange = { activeField = if (it) ActiveField.Account else null },
                    predictedOptions = predictedAccountOptions,
                    createLabel = accountCreateLabel,
                    onCreate = if (onCreateAccount != null) {
                        { query -> openCreateAccount(query, false) }
                    } else {
                        null
                    },
                )
            }
        }

        if (!editing) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (activeField == null || activeField == ActiveField.Description) {
            val visibleSuggestions = remember(state.description, suggestions.descriptions) {
                state.description.trim().let { query ->
                    val matches = if (query.isEmpty()) {
                        suggestions.descriptions
                    } else {
                        suggestions.descriptions.filter { it.contains(query, ignoreCase = true) }
                    }
                    matches.filterNot { it.equals(query, ignoreCase = true) }
                }
            }
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(Res.string.entry_description_optional)) },
                singleLine = true,
                trailingIcon = if (state.description.isNotEmpty()) {
                    { ClearIconButton(onClear = { onDescriptionChange("") }) }
                } else {
                    null
                },
                modifier = Modifier
                    .onFocusChanged { activeField = if (it.isFocused) ActiveField.Description else null }
                    .fillMaxWidth(),
            )
            if (activeField == ActiveField.Description && visibleSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OptionChipRow(
                    options = visibleSuggestions,
                    optionLabel = { it },
                    onSelect = { suggestion ->
                        onDescriptionChange(suggestion)
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                )
            }
            // Ok button to confirm the description without using the back arrow
            if (activeField == ActiveField.Description) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                    ) {
                        Text(stringResource(Res.string.entry_ok))
                    }
                }
            }
        }

        if (!editing) {
            Spacer(modifier = Modifier.height(16.dp))
            DateAndTimeButtons(
                createdAt = state.createdAt,
                createdZone = state.createdZone,
                onChange = onCreatedAtChange,
            )

            // Quick entries fill the form from a past entry but keep the current date and time.
            if (suggestions.quickEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.entry_quick_entry),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                val quickChips = remember(suggestions.quickEntries) { quickEntryChips(suggestions.quickEntries) }
                OptionChipRow(
                    options = quickChips,
                    optionLabel = { it.label },
                    onSelect = { onQuickEntry(it.entry) },
                    dense = true,
                    maxChipWidth = QuickEntryChipMaxWidth,
                    optionIcon = { quickEntryIcon(it.entry) },
                    optionColors = { if (it.entry.cadence != null) recurringChipColors else null },
                    optionContentDescription = {
                        if (it.entry.cadence != null) recurringChipDescription else null
                    },
                )
            }
        }
    }

    AnimatedVisibility(
        visible = activeField == ActiveField.Amount,
        enter = slideInVertically(tween(220)) { it / 4 } + fadeIn(tween(220)),
        exit = fadeOut(tween(120)),
    ) {
        EntryAmountCalculator(
            isToAmount = false,
            state = state,
            fromAccount = fromAccount,
            toAccount = toAccount,
            purpose = amountLabel,
            warning = amountPositiveWarning,
            onCommit = onAmountChange,
            onWarning = { amountWarning = it },
            onCancel = { activeField = null },
        )
    }

    AnimatedVisibility(
        visible = activeField == ActiveField.ToAmount,
        enter = slideInVertically(tween(220)) { it / 4 } + fadeIn(tween(220)),
        exit = fadeOut(tween(120)),
    ) {
        EntryAmountCalculator(
            isToAmount = true,
            state = state,
            fromAccount = fromAccount,
            toAccount = toAccount,
            purpose = receivedAmountLabel,
            warning = amountPositiveWarning,
            onCommit = onToAmountChange,
            onWarning = { toAmountWarning = it },
            onCancel = { activeField = null },
        )
    }

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
