package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.accounts_duplicate_name
import org.sjbtimdan.linden.resources.categories_duplicate_name
import org.sjbtimdan.linden.resources.common_category
import org.sjbtimdan.linden.resources.common_clear
import org.sjbtimdan.linden.resources.common_invalid_amount
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
import org.sjbtimdan.linden.resources.entry_to_account
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.accounts.AccountDialog
import org.sjbtimdan.linden.ui.accounts.AccountDialogState
import org.sjbtimdan.linden.ui.categories.CategoryDialog
import org.sjbtimdan.linden.ui.categories.CategoryDialogState
import kotlin.time.Instant

/** Which field is expanded right now; while one is, the rest of the form collapses. */
private enum class ActiveField { Description, From, To, Category, Account, Amount, ToAmount }

/** One-line context for the keypad takeover: the field's purpose plus the accounts involved. */
private fun calculatorContextLabel(purpose: String, from: Account?, to: Account?): String = when {
    from != null && to != null ->
        "$purpose · ${from.name} · ${from.currency.symbol} → ${to.name} · ${to.currency.symbol}"

    from != null -> "$purpose · ${from.name} · ${from.currency.symbol}"

    to != null -> "$purpose · ${to.name} · ${to.currency.symbol}"

    else -> purpose
}

/** The category type a new category should get for an entry of [type]. */
private fun EntryType.toCategoryType(): CategoryType = when (this) {
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
    editEpoch: Int = 0,
    descriptionSuggestions: List<String> = emptyList(),
    accountSuggestions: List<Long> = emptyList(),
    categorySuggestions: List<Long> = emptyList(),
    quickEntries: List<QuickEntry> = emptyList(),
    onQuickEntry: (QuickEntry) -> Unit = {},
    defaultCurrency: Currency = Currency.CHF,
    onCreateCategory: ((String, CategoryType, CategoryIcon?) -> Boolean)? = null,
    onCreateAccount: ((String, Currency, Long, Boolean) -> Boolean)? = null,
) {
    val visibleCategories = categoriesForType(categories, state.type)
    val fromAccount = accounts.firstOrNull { it.id == state.accountId }
    val toAccount = accounts.firstOrNull { it.id == state.toAccountId }
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
    val duplicateCategoryNameError = stringResource(Res.string.categories_duplicate_name)
    val duplicateAccountNameError = stringResource(Res.string.accounts_duplicate_name)
    val invalidAmountError = stringResource(Res.string.common_invalid_amount)

    // Create dialogs opened from the "+ New" chips.
    var createCategoryState by remember { mutableStateOf<CategoryDialogState?>(null) }
    var createAccountState by remember { mutableStateOf<AccountDialogState?>(null) }
    val openCreateCategory: (String) -> Unit = { query ->
        createCategoryState = CategoryDialogState(
            category = null,
            name = query,
            type = state.type.toCategoryType(),
        )
    }
    val openCreateAccount: (String, Boolean) -> Unit = { query, selectAsTo ->
        createAccountState = AccountDialogState(
            account = null,
            name = query,
            currency = defaultCurrency,
            initialBalanceText = "",
            selectAsTo = selectAsTo,
        )
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
        }

        if (state.type == EntryType.Transfer) {
            if (!editing || activeField == ActiveField.From) {
                Spacer(modifier = Modifier.height(16.dp))
                FieldDropdown(
                    label = stringResource(Res.string.entry_from_account),
                    selected = accounts.firstOrNull { it.id == state.accountId },
                    options = accounts,
                    optionLabel = { it.name },
                    onSelect = { onAccountChange(it.id) },
                    onFocusChange = { activeField = if (it) ActiveField.From else null },
                    createLabel = if (onCreateAccount != null) {
                        { query -> if (query.isBlank()) newAccountText else newAccountWithName.format(query) }
                    } else {
                        null
                    },
                    onCreate = if (onCreateAccount != null) {
                        { query -> openCreateAccount(query, false) }
                    } else {
                        null
                    },
                )
            }
            if (!editing || activeField == ActiveField.To) {
                Spacer(modifier = Modifier.height(16.dp))
                FieldDropdown(
                    label = stringResource(Res.string.entry_to_account),
                    selected = accounts.firstOrNull { it.id == state.toAccountId },
                    options = accounts.filter { it.id != state.accountId },
                    optionLabel = { it.name },
                    onSelect = { onToAccountChange(it.id) },
                    onFocusChange = { activeField = if (it) ActiveField.To else null },
                    createLabel = if (onCreateAccount != null) {
                        { query -> if (query.isBlank()) newAccountText else newAccountWithName.format(query) }
                    } else {
                        null
                    },
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
                FieldDropdown(
                    label = stringResource(Res.string.common_category),
                    selected = visibleCategories.firstOrNull { it.id == state.categoryId },
                    options = visibleCategories,
                    optionLabel = { it.name },
                    onSelect = { onCategoryChange(it.id) },
                    onFocusChange = { activeField = if (it) ActiveField.Category else null },
                    predicted = categorySuggestions
                        .filterNot { it == state.categoryId }
                        .mapNotNull { id -> visibleCategories.firstOrNull { it.id == id } },
                    optionIcon = { it.icon?.imageVector() },
                    createLabel = if (onCreateCategory != null) {
                        { query -> if (query.isBlank()) newCategoryText else newCategoryWithName.format(query) }
                    } else {
                        null
                    },
                    onCreate = if (onCreateCategory != null) {
                        { query -> openCreateCategory(query) }
                    } else {
                        null
                    },
                )
            }
            if (!editing || activeField == ActiveField.Account) {
                Spacer(modifier = Modifier.height(16.dp))
                FieldDropdown(
                    label = stringResource(Res.string.entry_account),
                    selected = accounts.firstOrNull { it.id == state.accountId },
                    options = accounts,
                    optionLabel = { it.name },
                    onSelect = { onAccountChange(it.id) },
                    onFocusChange = { activeField = if (it) ActiveField.Account else null },
                    predicted = accountSuggestions
                        .filterNot { it == state.accountId }
                        .mapNotNull { id -> accounts.firstOrNull { it.id == id } },
                    createLabel = if (onCreateAccount != null) {
                        { query -> if (query.isBlank()) newAccountText else newAccountWithName.format(query) }
                    } else {
                        null
                    },
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
            val visibleSuggestions = state.description.trim().let { query ->
                val matches = if (query.isEmpty()) {
                    descriptionSuggestions
                } else {
                    descriptionSuggestions.filter { it.contains(query, ignoreCase = true) }
                }
                matches.filterNot { it.equals(query, ignoreCase = true) }
            }
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(Res.string.entry_description_optional)) },
                singleLine = true,
                trailingIcon = if (state.description.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onDescriptionChange("") },
                        ) { Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.common_clear)) }
                    }
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
            if (quickEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.entry_quick_entry),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OptionChipRow(
                    options = quickEntries,
                    optionLabel = ::quickEntryLabel,
                    onSelect = onQuickEntry,
                )
            }
        }
    }

    if (activeField == ActiveField.Amount) {
        AmountCalculator(
            initialMinor = state.amount,
            currencySymbol = fromAccount?.currency?.symbol,
            contextLabel = calculatorContextLabel(
                purpose = amountLabel,
                from = fromAccount,
                to = toAccount,
            ),
            onEnter = { value ->
                onAmountChange(value)
                activeField = null
            },
            onInvalid = {
                amountWarning = amountPositiveWarning
                activeField = null
            },
            onCancel = { activeField = null },
        )
    }

    if (activeField == ActiveField.ToAmount) {
        AmountCalculator(
            initialMinor = state.toAmount,
            currencySymbol = toAccount?.currency?.symbol,
            contextLabel = calculatorContextLabel(
                purpose = receivedAmountLabel,
                from = fromAccount,
                to = toAccount,
            ),
            onEnter = { value ->
                onToAmountChange(value)
                activeField = null
            },
            onInvalid = {
                toAmountWarning = amountPositiveWarning
                activeField = null
            },
            onCancel = { activeField = null },
        )
    }

    createCategoryState?.let { dialogState ->
        CategoryDialog(
            name = dialogState.name,
            type = dialogState.type,
            icon = dialogState.icon,
            nameError = dialogState.nameError,
            isEditing = false,
            canDelete = false,
            onNameChange = { createCategoryState = dialogState.copy(name = it, nameError = null) },
            onTypeChange = { createCategoryState = dialogState.copy(type = it) },
            onIconChange = { createCategoryState = dialogState.copy(icon = it) },
            onDelete = {},
            onSave = {
                val name = dialogState.name.trim()
                if (name.isNotEmpty()) {
                    val saved = onCreateCategory?.invoke(name, dialogState.type, dialogState.icon) ?: false
                    if (saved) {
                        createCategoryState = null
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    } else {
                        createCategoryState = dialogState.copy(nameError = duplicateCategoryNameError)
                    }
                }
            },
            onDismiss = { createCategoryState = null },
        )
    }

    createAccountState?.let { dialogState ->
        AccountDialog(
            name = dialogState.name,
            currency = dialogState.currency,
            initialBalanceText = dialogState.initialBalanceText,
            nameError = dialogState.nameError,
            initialBalanceError = dialogState.initialBalanceError,
            isEditing = false,
            canChangeCurrency = true,
            canDelete = false,
            hidden = false,
            onNameChange = { createAccountState = dialogState.copy(name = it, nameError = null) },
            onCurrencyChange = { createAccountState = dialogState.copy(currency = it) },
            onInitialBalanceChange = {
                createAccountState = dialogState.copy(initialBalanceText = it, initialBalanceError = null)
            },
            onHiddenChange = {},
            onDelete = {},
            onSave = {
                val name = dialogState.name.trim()
                if (name.isNotEmpty()) {
                    // Blank means zero; any other unparseable value is an error, not zero.
                    val initialBalance = if (dialogState.initialBalanceText.isBlank()) {
                        0L
                    } else {
                        parseAmount(dialogState.initialBalanceText)
                    }
                    if (initialBalance == null) {
                        createAccountState = dialogState.copy(initialBalanceError = invalidAmountError)
                    } else {
                        val saved = onCreateAccount?.invoke(
                            name,
                            dialogState.currency,
                            initialBalance,
                            dialogState.selectAsTo,
                        ) ?: false
                        if (saved) {
                            createAccountState = null
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        } else {
                            createAccountState = dialogState.copy(nameError = duplicateAccountNameError)
                        }
                    }
                }
            },
            onDismiss = { createAccountState = null },
        )
    }
}
