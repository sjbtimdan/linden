package org.sjbtimdan.linden.ui.entry

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.entry_req_account
import org.sjbtimdan.linden.resources.entry_req_amount
import org.sjbtimdan.linden.resources.entry_req_category
import org.sjbtimdan.linden.resources.entry_req_destination
import org.sjbtimdan.linden.resources.entry_req_different_destination
import org.sjbtimdan.linden.resources.entry_req_no_accounts
import org.sjbtimdan.linden.resources.entry_req_no_categories
import org.sjbtimdan.linden.resources.entry_req_received
import org.sjbtimdan.linden.resources.entry_req_source

/**
 * The first requirement a draft still misses. Pure, so it can be unit-tested
 * without resources; UI resolves the localized wording via [text].
 *
 * [NO_ACCOUNTS] and [NO_CATEGORY] are the "the form cannot be satisfied at all"
 * blockers: nothing exists to pick, so the fix is to create it (the UI renders
 * them as an action that opens the create dialog, not as a picker hint).
 */
enum class MissingRequirement {
    AMOUNT,
    ACCOUNT,
    SOURCE_ACCOUNT,
    CATEGORY,
    DESTINATION_ACCOUNT,
    DIFFERENT_DESTINATION,
    RECEIVED_AMOUNT,
    NO_ACCOUNTS,
    NO_CATEGORY,
}

/** Localized wording for [MissingRequirement]. */
@Composable
internal fun MissingRequirement.text(): String = stringResource(
    when (this) {
        MissingRequirement.AMOUNT -> Res.string.entry_req_amount
        MissingRequirement.ACCOUNT -> Res.string.entry_req_account
        MissingRequirement.SOURCE_ACCOUNT -> Res.string.entry_req_source
        MissingRequirement.CATEGORY -> Res.string.entry_req_category
        MissingRequirement.DESTINATION_ACCOUNT -> Res.string.entry_req_destination
        MissingRequirement.DIFFERENT_DESTINATION -> Res.string.entry_req_different_destination
        MissingRequirement.RECEIVED_AMOUNT -> Res.string.entry_req_received
        MissingRequirement.NO_ACCOUNTS -> Res.string.entry_req_no_accounts
        MissingRequirement.NO_CATEGORY -> Res.string.entry_req_no_categories
    },
)

/**
 * True when the blocker cannot be resolved by picking an existing option — the
 * hint must offer the create-dialog action rather than point at a dropdown.
 */
internal val MissingRequirement.isCreateAction: Boolean
    get() = this == MissingRequirement.NO_ACCOUNTS || this == MissingRequirement.NO_CATEGORY

/**
 * Why the current draft cannot be saved yet, or null when the form is valid.
 *
 * When the form can be satisfied by picking existing options (accounts exist,
 * categories of the right type exist, a transfer has two accounts) this returns
 * the first field the draft still misses, e.g. [MissingRequirement.AMOUNT].
 *
 * When it cannot — no accounts, no matching categories, or only one account for
 * a transfer — this returns [MissingRequirement.NO_ACCOUNTS] or
 * [MissingRequirement.NO_CATEGORY] instead of a picker hint, so the UI can
 * offer the create action directly ("choose an account" would be misleading
 * when there is nothing to choose).
 */
internal fun missingRequirement(
    draft: EntryDraft?,
    accounts: List<Account>,
    categories: List<Category>,
): MissingRequirement? {
    val state = draft ?: return null
    when (state.type) {
        EntryType.Transfer -> if (accounts.size < 2) return MissingRequirement.NO_ACCOUNTS

        EntryType.Expense, EntryType.Income -> when {
            accounts.isEmpty() -> return MissingRequirement.NO_ACCOUNTS
            categoriesForType(categories, state.type).isEmpty() -> return MissingRequirement.NO_CATEGORY
        }
    }
    return state.firstMissingRequirement(accounts)
}
