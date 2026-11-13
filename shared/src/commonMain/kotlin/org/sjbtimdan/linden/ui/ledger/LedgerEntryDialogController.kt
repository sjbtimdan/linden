package org.sjbtimdan.linden.ui.ledger

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.time.AppClock
import org.sjbtimdan.linden.ui.entry.EntryDraft
import org.sjbtimdan.linden.ui.entry.categoriesForType
import kotlin.time.Instant

/**
 * Owns the ledger's new/edit entry dialog: the edited draft, the saving guard,
 * duplicate, and the undo slot for a deleted entry. Keeping this out of
 * [LedgerViewModel] stops period, filter and aggregation state from sharing a
 * class with the dialog write path.
 */
internal class LedgerEntryDialogController(
    private val entryDao: EntryDao,
    private val draftState: MutableStateFlow<EntryDraft?>,
    private val accounts: StateFlow<List<Account>>,
    private val categories: StateFlow<List<Category>>,
    private val savingState: MutableStateFlow<Boolean>,
    private val clock: AppClock,
    private val zone: TimeZone,
    private val write: (suspend () -> Unit) -> Unit,
) {
    private val _lastDeleted = MutableStateFlow<Entry?>(null)

    /** The entry most recently deleted from the edit dialog, held so the delete can be undone. */
    val lastDeleted: StateFlow<Entry?> = _lastDeleted.asStateFlow()

    fun open(entry: Entry) {
        draftState.value = EntryDraft.forEdit(entry)
    }

    /**
     * Flips the entry being edited between Expense and Income, carrying over the
     * amount, description and date. The category is kept only when it can back
     * the target type; an incompatible one is cleared so the form asks for a
     * valid pick. Transfers are never offered the toggle, so they are never
     * switched.
     */
    fun changeType(type: EntryType) {
        draftState.update { state ->
            if (state == null || state.type == type) return@update state
            val keepCategory = categoriesForType(categories.value, type).any { it.id == state.categoryId }
            state.copy(
                type = type,
                categoryId = state.categoryId?.takeIf { keepCategory },
            )
        }
    }

    /**
     * Turns the dialog into a New entry draft that copies the fields of the entry
     * being edited but is dated [now] in [zone]: Save then creates a duplicate,
     * leaving the original untouched. The edit is replaced, not stacked.
     */
    fun duplicate(now: Instant = clock.now(), zone: TimeZone = this.zone) {
        draftState.update { it?.asNewEntry(now, zone) }
    }

    /**
     * Saves the dialog draft and closes the dialog once the write succeeds; the
     * dialog stays open on failure so the draft is not lost. Returns false when
     * there is nothing to save or the draft is invalid.
     */
    fun save(): Boolean {
        val state = draftState.value ?: return false
        val entry = state.toEntry(accounts.value, categories.value) ?: return false
        if (savingState.value) return false
        savingState.value = true
        write {
            try {
                // Duplicate turns the dialog into a New draft (no edited entry), which creates.
                if (state.editing == null) {
                    entryDao.create(entry)
                } else {
                    entryDao.update(entry)
                }
                draftState.value = null
            } finally {
                savingState.value = false
            }
        }
        return true
    }

    /** Deletes the entry being edited and closes the dialog once the write succeeds. */
    fun delete() {
        val entry = draftState.value?.editing ?: return
        if (savingState.value) return
        savingState.value = true
        write {
            try {
                entryDao.delete(entry.id)
                _lastDeleted.value = entry
                draftState.value = null
            } finally {
                savingState.value = false
            }
        }
    }

    /**
     * Re-inserts the entry removed by [delete], so an accidental delete can be
     * taken back. Every field is restored, including the timestamps; the row
     * gets a fresh id, as nothing references entry ids.
     */
    fun undoLastDeleted() {
        val entry = _lastDeleted.value ?: return
        _lastDeleted.value = null
        write { entryDao.create(entry) }
    }

    /** Dismisses the undo offer, making the last delete final. */
    fun clearLastDeleted() {
        _lastDeleted.value = null
    }

    fun dismiss() {
        draftState.value = null
    }
}
