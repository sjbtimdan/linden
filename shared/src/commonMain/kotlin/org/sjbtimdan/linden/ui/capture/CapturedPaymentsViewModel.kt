package org.sjbtimdan.linden.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.capture.CaptureRepository
import org.sjbtimdan.linden.capture.CaptureRule
import org.sjbtimdan.linden.capture.PendingCapture
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.time.AppClock
import org.sjbtimdan.linden.ui.entry.EntryDraft
import org.sjbtimdan.linden.ui.entry.formatAmount
import org.sjbtimdan.linden.util.stateFlow

/**
 * Backs the captured-payments inbox: the pending captures resolved into rows
 * (amount text, mapped account), and the prefill that turns a reviewed capture
 * into a draft on the entry form.
 */
class CapturedPaymentsViewModel(
    private val repository: CaptureRepository,
    accountDao: AccountDao,
    private val clock: AppClock,
) : ViewModel() {
    private val pendingCaptures = MutableStateFlow<List<PendingCapture>>(emptyList())

    private val rules: StateFlow<List<CaptureRule>> = repository.rulesFlow().stateFlow(viewModelScope, emptyList())

    private val accounts: StateFlow<List<Account>> = accountDao.getAll().stateFlow(viewModelScope, emptyList())

    private val seenCaptureId: StateFlow<Long> = repository.seenCaptureIdFlow().stateFlow(viewModelScope, 0L)

    val items: StateFlow<List<CapturedPaymentItem>> = combine(
        pendingCaptures,
        rules,
        accounts,
    ) { pending, rules, accounts ->
        pending.map { capture -> rowFor(capture, rules, accounts) }
    }.stateFlow(viewModelScope, emptyList())

    /** True while a capture newer than the acknowledged watermark waits for review. */
    val hasUnseen: StateFlow<Boolean> = combine(pendingCaptures, seenCaptureId) { pending, seen ->
        pending.any { it.id > seen }
    }.stateFlow(viewModelScope, false)

    init {
        viewModelScope.launch { repository.pendingFlow().collect { pendingCaptures.value = it } }
    }

    /**
     * Re-reads the inbox. The notification listener writes through its own
     * database connection, which this flow cannot observe, so the screen calls
     * this when it resumes.
     */
    fun refresh() {
        viewModelScope.launch { pendingCaptures.value = repository.pending() }
    }

    fun remove(id: Long) {
        viewModelScope.launch { repository.removePending(id) }
    }

    /**
     * Acknowledges everything pending now, so the banner collapses to its bar
     * until a newer capture arrives.
     */
    fun acknowledge() {
        viewModelScope.launch {
            pendingCaptures.value.maxOfOrNull { it.id }?.let { repository.markSeen(it) }
        }
    }

    /**
     * The draft a reviewed capture loads into the entry form: amount and
     * description prefilled, account set when the rule still maps to a visible
     * one — the user still picks a category and confirms. Saving the draft
     * consumes the capture; abandoning it leaves the capture pending.
     */
    fun buildDraft(capture: PendingCapture): EntryDraft {
        val row = rowFor(capture, rules.value, accounts.value)
        return EntryDraft.forNew(capture.type, clock = clock).copy(
            amountText = row.amountText,
            description = capture.description,
            accountId = row.accountId,
            sourceCaptureId = capture.id,
        )
    }

    private fun rowFor(
        capture: PendingCapture,
        rules: List<CaptureRule>,
        accounts: List<Account>,
    ): CapturedPaymentItem {
        val accountId = rules.firstOrNull { it.packageName == capture.packageName }?.accountId
        val account = accounts.firstOrNull { it.id == accountId && !it.hidden }
        return CapturedPaymentItem(
            capture = capture,
            amountText = formatAmount(
                capture.amountMinor,
                capture.currency?.decimalDigits ?: account?.currency?.decimalDigits ?: 2,
            ),
            accountName = account?.name,
            accountId = account?.id,
        )
    }
}
