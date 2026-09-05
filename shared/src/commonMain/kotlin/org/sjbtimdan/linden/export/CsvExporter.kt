package org.sjbtimdan.linden.export

import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.TransferEntry
import java.io.OutputStream

private const val CSV_HEADER = "type,date,account,category,description,amount,currency,toAccount,toAmount,toCurrency"

/**
 * Serializes [entries] to CSV (RFC 4180) with a header row.
 *
 * Amounts are exported as decimal strings (e.g. "4.50") derived from integer
 * minor units via integer math — never floating point. Fields are quoted and
 * escaped per RFC 4180, so descriptions containing commas, quotes or newlines
 * survive a round trip into a spreadsheet.
 */
fun entriesToCsv(entries: List<Entry>): String = buildString {
    appendLine(CSV_HEADER)
    entries.forEach { entry ->
        appendLine(
            listOf(
                entry.type.name,
                entry.createdAt.toString(),
                entry.account.name,
                entry.category?.name.orEmpty(),
                entry.description.orEmpty(),
                formatAmount(entry.amount),
                entry.account.currency.name,
                toAccountName(entry),
                toAmount(entry),
                toCurrency(entry),
            ).joinToString(",") { escapeCsv(it) },
        )
    }
}

class CsvExportManager(private val entryDao: EntryDao) {
    suspend fun exportTo(output: OutputStream) {
        val entries = entryDao.getAll().first()
        output.use { it.write(entriesToCsv(entries).encodeToByteArray()) }
    }
}

private fun toAccountName(entry: Entry): String = when (entry) {
    is TransferEntry -> entry.toAccount.name
    else -> ""
}

private fun toAmount(entry: Entry): String = when (entry) {
    is TransferEntry -> entry.toAmount?.let(::formatAmount).orEmpty()
    else -> ""
}

private fun toCurrency(entry: Entry): String = when (entry) {
    is TransferEntry -> entry.toAccount.currency.name
    else -> ""
}

/** Formats minor units as a fixed two-decimal string, e.g. 450 -> "4.50". */
private fun formatAmount(minorUnits: Long): String {
    val sign = if (minorUnits < 0) "-" else ""
    val abs = kotlin.math.abs(minorUnits)
    return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
}

/** Quotes a field if it contains a comma, quote or newline; doubles embedded quotes. */
private fun escapeCsv(field: String): String {
    if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        return "\"" + field.replace("\"", "\"\"") + "\""
    }
    return field
}
