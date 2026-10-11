package org.sjbtimdan.linden.inflation

import org.sjbtimdan.linden.model.Currency

/** Parses the chunked integer literals emitted by tools/gen_cpi.py. */
internal fun ints(vararg chunks: String): IntArray =
    chunks.joinToString("").trimEnd(',').split(',').map { it.toInt() }.toIntArray()

/** The bundled CPI snapshot (CpiSeedData), keyed by the currencies the app knows. */
internal fun seedSeries(): Map<Currency, CpiSeries> = CpiSeedData.series.mapNotNull { (code, series) ->
    Currency.entries.firstOrNull { it.name == code }?.let { currency -> currency to series }
}.toMap()
