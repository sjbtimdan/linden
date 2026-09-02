package org.sjbtimdan.linden.ui.entry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
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
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.predictions.DescriptionPredictionInput
import org.sjbtimdan.linden.predictions.FieldPredictionInput
import org.sjbtimdan.linden.predictions.PREDICTION_HORIZON_MONTHS
import org.sjbtimdan.linden.predictions.PREDICTION_TOP_N
import org.sjbtimdan.linden.predictions.QUICK_ENTRY_TOP_N
import org.sjbtimdan.linden.predictions.QuickEntry
import org.sjbtimdan.linden.predictions.predictAccounts
import org.sjbtimdan.linden.predictions.predictCategories
import org.sjbtimdan.linden.predictions.predictDescriptions
import org.sjbtimdan.linden.predictions.predictQuickEntries
import kotlin.time.Clock

private const val DESCRIPTION_DEBOUNCE_MILLIS = 150L

/**
 * Suggestion flows for a new entry: the most likely accounts, categories,
 * descriptions and quick-entry chips for the current draft, recomputed whenever
 * it changes. The field predictions only consider entries of the draft's type
 * from the last [PREDICTION_HORIZON_MONTHS] months, matching the predictors'
 * data contract; quick entry considers all of them because it ranks by time of
 * day rather than recency. Editing an existing entry never predicts. Predictions
 * run on the default dispatcher and description keystrokes are debounced, so
 * typing never recomputes on the main thread.
 *
 * Kept outside [EntryEditorViewModel] so ViewModels whose dialog never shows
 * suggestions (history) don't pay for the extra entry window and its flows.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class EntrySuggestionsProvider(
    private val entryDao: EntryDao,
    private val draft: StateFlow<EntryDraft?>,
    private val scope: CoroutineScope,
    descriptionDebounceMillis: Long = DESCRIPTION_DEBOUNCE_MILLIS,
) {
    /** Entries of the draft's type from the last [PREDICTION_HORIZON_MONTHS] months. */
    private val predictionEntries: StateFlow<List<Entry>> = typeEntries { type ->
        entryDao.getSinceByType(type, horizonCutoffMillis())
    }

    /**
     * All entries of the draft's type, no date cutoff: quick entry ranks by
     * time of day, so entries outside the prediction horizon must be candidates.
     */
    private val allTypeEntries: StateFlow<List<Entry>> = typeEntries(entryDao::getAllByType)

    /** The draft's description, trailing the field by the debounce while it is enabled. */
    private val debouncedDescription: Flow<String> = draft
        .map { it?.description.orEmpty() }
        .distinctUntilChanged()
        .let { descriptions ->
            if (descriptionDebounceMillis <= 0) {
                descriptions
            } else {
                descriptions.debounce(descriptionDebounceMillis)
            }
        }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = draft.value?.description.orEmpty())

    /**
     * The draft with its description trailing the field by
     * [descriptionDebounceMillis], so suggestions only recompute once typing
     * pauses. The current description is emitted immediately via the initial
     * value; a debounce of zero disables the delay for tests.
     */
    private val debouncedDraft: StateFlow<EntryDraft?> = combine(
        draft,
        debouncedDescription,
    ) { state, description -> state?.copy(description = description) }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = null)

    /** Most likely account ids for the current draft. */
    val accountSuggestions: StateFlow<List<Long>> = suggestion(predictionEntries) { state, entries ->
        predictAccounts(
            entries = entries,
            input = state.fieldInput(),
            now = Clock.System.now(),
            timeZone = TimeZone.currentSystemDefault(),
            topN = PREDICTION_TOP_N,
        )
    }

    /** Most likely category ids for the current draft. */
    val categorySuggestions: StateFlow<List<Long>> = suggestion(predictionEntries) { state, entries ->
        predictCategories(
            entries = entries,
            input = state.fieldInput(),
            now = Clock.System.now(),
            timeZone = TimeZone.currentSystemDefault(),
            topN = PREDICTION_TOP_N,
        )
    }

    /** Most likely descriptions for the current draft. */
    val descriptionSuggestions: StateFlow<List<String>> = suggestion(predictionEntries) { state, entries ->
        predictDescriptions(
            entries = entries,
            input = DescriptionPredictionInput(
                type = state.type,
                categoryId = state.categoryId,
                accountId = state.accountId,
                amount = state.amount,
                description = state.description,
            ),
            now = Clock.System.now(),
            timeZone = TimeZone.currentSystemDefault(),
            topN = PREDICTION_TOP_N,
        )
    }

    /** Whole entries the user is likely to repeat right now, ranked time first. */
    val quickEntries: StateFlow<List<QuickEntry>> = suggestion(allTypeEntries) { state, entries ->
        predictQuickEntries(
            entries = entries,
            input = state.fieldInput(),
            now = Clock.System.now(),
            timeZone = TimeZone.currentSystemDefault(),
            topN = QUICK_ENTRY_TOP_N,
        )
    }

    /** Entries of the draft's type, loaded via [load] whenever the type changes. */
    private fun typeEntries(load: (EntryType) -> Flow<List<Entry>>): StateFlow<List<Entry>> = draft
        .map { it?.type }
        .distinctUntilChanged()
        .flatMapLatest { type ->
            if (type == null) {
                flowOf(emptyList())
            } else {
                load(type)
            }
        }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /** Suggestion list for the current draft, recomputed when it or [entries] change; empty while editing. */
    private fun <T> suggestion(
        entries: StateFlow<List<Entry>>,
        compute: (EntryDraft, List<Entry>) -> List<T>,
    ): StateFlow<List<T>> = combine(debouncedDraft, entries) { state, history ->
        if (state == null || state.editing != null) {
            emptyList()
        } else {
            compute(state, history)
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
