package org.sjbtimdan.linden.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.capture.CaptureRepository
import org.sjbtimdan.linden.capture.CaptureRule
import org.sjbtimdan.linden.capture.CaptureTemplate
import org.sjbtimdan.linden.capture.PendingCapture
import org.sjbtimdan.linden.capture.matchCaptureTemplate
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.time.AppClock
import org.sjbtimdan.linden.ui.entry.EntryDraft
import org.sjbtimdan.linden.ui.entry.categoriesForType
import org.sjbtimdan.linden.ui.entry.formatAmount
import org.sjbtimdan.linden.util.stateFlow

/**
 * Backs the captured-payments inbox: the pending captures resolved into rows
 * (amount text, mapped account), and the prefill that turns a reviewed capture
 * into a draft on the entry form — including the category, account and type of
 * a learned template for the same notification wording.
 */
class CapturedPaymentsViewModel(
    private val repository: CaptureRepository,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
    private val clock: AppClock,
) : ViewModel() {
    private val pendingCaptures = MutableStateFlow<List<PendingCapture>>(emptyList())

    private val rules: StateFlow<List<CaptureRule>> = repository.rulesFlow().stateFlow(viewModelScope, emptyList())

    private val accounts: StateFlow<List<Account>> = accountDao.getAll().stateFlow(viewModelScope, emptyList())

    private val categories: StateFlow<List<Category>> = categoryDao.getAll().stateFlow(viewModelScope, emptyList())

    private val templates: StateFlow<List<CaptureTemplate>> =
        repository.templatesFlow().stateFlow(viewModelScope, emptyList())

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
        // The listener's own connection is invisible to pendingFlow, so its
        // write only reaches this view model through the process-wide tick.
        viewModelScope.launch {
            CaptureRepository.inboxChanges.collect { pendingCaptures.value = repository.pending() }
        }
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
     * description prefilled, account set when the rule (or a learned template)
     * maps to a visible one — the user still confirms. A learned template for
     * the capture's package and notification wording also fills the type and
     * the category the user chose last time, each only while it still exists
     * and fits the type. Saving the draft consumes the capture; abandoning it
     * leaves the capture pending.
     */
    fun buildDraft(capture: PendingCapture): EntryDraft {
        val template = matchCaptureTemplate(capture, templates.value)
        val type = template?.type ?: capture.type
        val accounts = accounts.value
        val ruleAccountId = rules.value.firstOrNull { it.packageName == capture.packageName }?.accountId
        val accountId = listOfNotNull(template?.accountId, ruleAccountId)
            .firstNotNullOfOrNull { id -> accounts.firstOrNull { it.id == id && !it.hidden }?.id }
        val categoryId = template?.categoryId
            ?.takeIf { id -> categoriesForType(categories.value, type).any { it.id == id } }
        val decimalDigits = capture.currency?.decimalDigits
            ?: accountId?.let { id -> accounts.firstOrNull { it.id == id }?.currency?.decimalDigits }
            ?: 2
        return EntryDraft.forNew(type, clock = clock).copy(
            amountText = formatAmount(capture.amountMinor, decimalDigits),
            description = template?.description ?: capture.description,
            accountId = accountId,
            categoryId = categoryId,
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
