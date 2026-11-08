package org.sjbtimdan.linden.ui.ledger

import androidx.compose.runtime.Immutable
import org.sjbtimdan.linden.model.Category

/** A category paired with its net total in the default currency (minor units) and entry count. */
@Immutable
data class CategoryWithTotal(
    val category: Category?,
    val total: Long,
    val count: Int,
    /** Monthly budget limit in the default currency's minor units; null when none is set or the period is not a month. */
    val budget: Long? = null,
)
