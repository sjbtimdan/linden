package org.sjbtimdan.linden.ui.inflation

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.sjbtimdan.linden.data.CpiDao
import org.sjbtimdan.linden.ui.AppViewModel

class InflationViewModel(cpiDao: CpiDao) : AppViewModel() {
    /** One row per currency with stored CPI months, in currency declaration order. */
    val rows: StateFlow<List<InflationRow>> = cpiDao.seriesFlow().map(::inflationRows).stateFlow(emptyList())
}
