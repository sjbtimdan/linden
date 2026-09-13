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
import org.sjbtimdan.linden.resources.entry_req_received
import org.sjbtimdan.linden.resources.entry_req_source

/**
 * The first requirement a draft still misses. Pure, so it can be unit-tested
 * without resources; UI resolves the localized wording via [text].
 */
enum class MissingRequirement {
    AMOUNT,
    ACCOUNT,
    SOURCE_ACCOUNT,
    CATEGORY,
    DESTINATION_ACCOUNT,
    DIFFERENT_DESTINATION,
    RECEIVED_AMOUNT,
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
    },
)

/**
 * Why the current draft cannot be saved yet, or null when the form is valid —
 * and also null when the form cannot be satisfied at all (no accounts or
 * categories, or only one account for a transfer): the empty dropdowns and
 * their "+ New" chips already explain the blocker, and "choose an account"
 * would be misleading when there is nothing to choose.
 */
internal fun missingRequirement(
    draft: EntryDraft?,
    accounts: List<Account>,
    categories: List<Category>,
): MissingRequirement? {
    val state = draft ?: return null
    val satisfiable = when (state.type) {
        EntryType.Transfer -> accounts.size >= 2

        EntryType.Expense, EntryType.Income -> accounts.isNotEmpty() && categoriesForType(
            categories,
            state.type,
        ).isNotEmpty()
    }
    if (!satisfiable) return null
    return state.firstMissingRequirement(accounts)
}
