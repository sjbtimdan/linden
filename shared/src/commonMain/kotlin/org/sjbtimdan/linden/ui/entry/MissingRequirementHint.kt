package org.sjbtimdan.linden.ui.entry

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.EntryType

/** Error-colored line naming the field that keeps the draft from being saved. */
@Composable
fun MissingRequirementHint(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier,
    )
}

/**
 * Why the current draft cannot be saved yet, or null when the form is valid —
 * and also null when the form's own [MissingFieldLink]s already explain the
 * blocker (no accounts or categories, or only one account for a transfer), so
 * the hint never duplicates the inline links.
 */
internal fun missingRequirementHint(draft: EntryDraft?, accounts: List<Account>, categories: List<Category>): String? {
    val state = draft ?: return null
    val satisfiable = when (state.type) {
        EntryType.Transfer -> accounts.size >= 2
        EntryType.Expense, EntryType.Income -> accounts.isNotEmpty() && hasUsableCategory(categories, state.type)
    }
    if (!satisfiable) return null
    return state.firstMissingRequirement(accounts)
}

/** Whether at least one category is selectable for [type], mirroring [EntryForm]'s list. */
private fun hasUsableCategory(categories: List<Category>, type: EntryType): Boolean = when (type) {
    EntryType.Expense -> categories.any { it.type != CategoryType.Income }
    EntryType.Income -> categories.any { it.type != CategoryType.Expense }
    EntryType.Transfer -> false
}
