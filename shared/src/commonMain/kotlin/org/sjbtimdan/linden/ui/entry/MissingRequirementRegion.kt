package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.accounts.AccountDialogState
import org.sjbtimdan.linden.ui.categories.CategoryDialogState

/**
 * Explains why Add is disabled, but only once the user has edited the form: a
 * fresh screen or the reset after a save is not an error. The blockers that
 * cannot be picked around (nothing to create an entry with) still show
 * immediately as their fix is to create one.
 */
@Composable
internal fun MissingRequirementRegion(
    draft: EntryDraft?,
    accounts: List<Account>,
    categories: List<Category>,
    draftEdited: Boolean,
    dialogs: EntryFormDialogs,
    onDialogsChange: (EntryFormDialogs) -> Unit,
    defaultCurrency: Currency,
) {
    val hint = missingRequirement(draft, accounts, categories)
        ?.takeIf { draftEdited || it.isCreateAction }
        ?: return

    // When nothing exists to pick, the hint becomes an action that opens the
    // same create dialogs as the form's "+ New" chips.
    MissingRequirementHint(
        message = hint.text(),
        onClick = when (hint) {
            MissingRequirement.NO_ACCOUNTS -> {
                {
                    onDialogsChange(
                        dialogs.copy(
                            account = AccountDialogState(
                                account = null,
                                name = "",
                                currency = defaultCurrency,
                                initialBalanceText = "",
                                selectAsTo = false,
                            ),
                        ),
                    )
                }
            }

            MissingRequirement.NO_CATEGORY -> {
                draft?.type?.let { type ->
                    {
                        onDialogsChange(
                            dialogs.copy(
                                category = CategoryDialogState(
                                    category = null,
                                    name = "",
                                    type = type.toCategoryType(),
                                ),
                            ),
                        )
                    }
                }
            }

            else -> null
        },
    )
    Spacer(modifier = Modifier.height(8.dp))
}
