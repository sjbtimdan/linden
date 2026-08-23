package org.sjbtimdan.linden.ui.entry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

/**
 * Suggestion flows for a new entry: the most likely accounts, categories and
 * descriptions for the current draft, recomputed whenever it changes. Only the
 * last [PREDICTION_HORIZON_MONTHS] months of entries are loaded, matching the
 * predictors' data contract; editing an existing entry never predicts.
 *
 * Kept outside [EntryEditorViewModel] so ViewModels whose dialog never shows
 * suggestions (history) don't pay for the extra entry window and its flows.
 */
class EntrySuggestionsProvider(
    entryDao: EntryDao,
    draft: StateFlow<EntryDraft?>,
    scope: CoroutineScope,
) {
    private val predictionEntries: StateFlow<List<Entry>> = entryDao
        .getSince(horizonCutoffMillis())
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /** Most likely account ids for the current draft. */
    val accountSuggestions: StateFlow<List<Long>> = combine(draft, predictionEntries) { state, entries ->
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
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /** Most likely category ids for the current draft. */
    val categorySuggestions: StateFlow<List<Long>> = combine(draft, predictionEntries) { state, entries ->
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
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /** Most likely descriptions for the current draft. */
    val descriptionSuggestions: StateFlow<List<String>> = combine(draft, predictionEntries) { state, entries ->
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
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

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
