package org.sjbtimdan.linden.ui.ledger

import kotlinx.datetime.LocalDate

/** The selected [LedgerPeriod] together with the anchor date its window is centered on. */
data class PeriodSelection(
    val period: LedgerPeriod,
    val anchor: LocalDate,
)
