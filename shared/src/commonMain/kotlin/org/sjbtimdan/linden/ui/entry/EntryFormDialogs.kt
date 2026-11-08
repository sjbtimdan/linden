package org.sjbtimdan.linden.ui.entry

import androidx.compose.runtime.Immutable
import org.sjbtimdan.linden.ui.accounts.AccountDialogState
import org.sjbtimdan.linden.ui.categories.CategoryDialogState

/** The create dialogs the entry form can show; a null slot means closed. */
@Immutable
data class EntryFormDialogs(
    val category: CategoryDialogState? = null,
    val account: AccountDialogState? = null,
)
