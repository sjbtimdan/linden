package org.sjbtimdan.linden.ui.entry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.predictions.DescriptionPredictionInput
import org.sjbtimdan.linden.predictions.FieldPredictionInput
import org.sjbtimdan.linden.predictions.PREDICTION_HORIZON_MONTHS
import org.sjbtimdan.linden.predictions.PREDICTION_TOP_N
import org.sjbtimdan.linden.predictions.predictAccounts
import org.sjbtimdan.linden.predictions.predictCategories
import org.sjbtimdan.linden.predictions.predictDescriptions
import kotlin.time.Clock

private const val DESCRIPTION_DEBOUNCE_MILLIS = 150L

/**
 * Suggestion flows for a new entry: the most likely accounts, categories and
 * descriptions for the current draft, recomputed whenever it changes. Only
 * entries of the draft's type from the last [PREDICTION_HORIZON_MONTHS] months
 * are loaded, matching the predictors' data contract; editing an existing
 * entry never predicts. Predictions run on the default dispatcher and
 * description keystrokes are debounced, so typing never recomputes on the
 * main thread.
 *
 * Kept outside [EntryEditorViewModel] so ViewModels whose dialog never shows
 * suggestions (history) don't pay for the extra entry window and its flows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EntrySuggestionsProvider(
    entryDao: EntryDao,
    draft: StateFlow<EntryDraft?>,
    scope: CoroutineScope,
    descriptionDebounceMillis: Long = DESCRIPTION_DEBOUNCE_MILLIS,
) {
    /** Entries of the draft's type from the last [PREDICTION_HORIZON_MONTHS] months. */
    private val predictionEntries: StateFlow<List<Entry>> = draft
        .map { it?.type }
        .distinctUntilChanged()
        .flatMapLatest { type ->
            if (type == null) {
                flowOf(emptyList())
            } else {
                entryDao.getSinceByType(type, horizonCutoffMillis())
            }
        }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /**
     * The draft with its description trailing the field by
     * [descriptionDebounceMillis], so suggestions only recompute once typing
     * pauses. The current description is emitted immediately via the initial
     * value; a debounce of zero disables the delay for tests.
     */
    private val debouncedDraft: StateFlow<EntryDraft?> = combine(
        draft,
        draft.map { it?.description.orEmpty() }
            .distinctUntilChanged()
            .let { descriptions ->
                if (descriptionDebounceMillis <= 0) {
                    descriptions
                } else {
                    descriptions.debounce(descriptionDebounceMillis)
                }
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = draft.value?.description.orEmpty(),
            ),
    ) { state, description -> state?.copy(description = description) }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = null)

    /** Most likely account ids for the current draft. */
    val accountSuggestions: StateFlow<List<Long>> = combine(debouncedDraft, predictionEntries) { state, entries ->
        if (state == null || state.editing != null) {
            emptyList()
        } else {
            predictAccounts(
                entries = entries,
                input = state.fieldInput(),
                now = Clock.System.now(),
                timeZone = TimeZone.currentSystemDefault(),
                topN = PREDICTION_TOP_N,
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /** Most likely category ids for the current draft. */
    val categorySuggestions: StateFlow<List<Long>> = combine(debouncedDraft, predictionEntries) { state, entries ->
        if (state == null || state.editing != null) {
            emptyList()
        } else {
            predictCategories(
                entries = entries,
                input = state.fieldInput(),
                now = Clock.System.now(),
                timeZone = TimeZone.currentSystemDefault(),
                topN = PREDICTION_TOP_N,
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /** Most likely descriptions for the current draft. */
    val descriptionSuggestions: StateFlow<List<String>> = combine(debouncedDraft, predictionEntries) { state, entries ->
        if (state == null || state.editing != null) {
            emptyList()
        } else {
            predictDescriptions(
                entries = entries,
                input = DescriptionPredictionInput(
                    type = state.type,
                    categoryId = state.categoryId,
                    accountId = state.accountId,
                    amount = state.amount,
                ),
                now = Clock.System.now(),
                timeZone = TimeZone.currentSystemDefault(),
                topN = PREDICTION_TOP_N,
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    private fun horizonCutoffMillis(): Long = Clock.System.now()
        .minus(PREDICTION_HORIZON_MONTHS, DateTimeUnit.MONTH, TimeZone.currentSystemDefault())
        .toEpochMilliseconds()

    private fun EntryDraft.fieldInput(): FieldPredictionInput = FieldPredictionInput(
        type = type,
        categoryId = categoryId,
        accountId = accountId,
        amount = amount,
        description = description,
    )
}
