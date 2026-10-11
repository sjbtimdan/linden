package org.sjbtimdan.linden.inflation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/**
 * A monthly consumer price index for one currency: integer index points scaled
 * by 100 (10650 = 106.50). The first value belongs to [firstMonth] ("YYYY-MM");
 * each following value advances one month. Only index ratios are meaningful, so
 * the reference period behind the points does not matter.
 */
class CpiSeries(
    val firstMonth: String,
    val values: IntArray,
) {
    private val firstYear: Int = firstMonth.substring(0, 4).toInt()
    private val firstMonthNumber: Int = firstMonth.substring(5, 7).toInt()

    /** The month of the last stored value, "YYYY-MM". */
    val latestMonth: String get() = monthAt(values.lastIndex)

    /** The "YYYY-MM" month [index] months after [firstMonth]. */
    fun monthAt(index: Int): String {
        val total = firstYear * 12 + (firstMonthNumber - 1) + index
        return "${total / 12}-${(total % 12 + 1).toString().padStart(2, '0')}"
    }

    /** Index at [date]'s month; dates outside the covered range clamp to its ends. */
    fun indexAt(date: LocalDate): Int {
        val monthOffset = (date.year - firstYear) * 12 + (date.month.number - firstMonthNumber)
        return values[monthOffset.coerceIn(0, values.lastIndex)]
    }

    /**
     * Percent change of the latest index against the index [monthsBack] months
     * earlier, or null when the series is not long enough.
     */
    fun changePercent(monthsBack: Int): Double? {
        if (monthsBack < 0) return null
        val then = values.getOrNull(values.lastIndex - monthsBack) ?: return null
        if (then == 0) return null
        return (values.last() - then) * 100.0 / then
    }
}
