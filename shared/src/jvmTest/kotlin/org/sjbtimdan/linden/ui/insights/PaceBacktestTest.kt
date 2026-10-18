package org.sjbtimdan.linden.ui.insights

import io.kotest.core.spec.style.StringSpec
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.sjbtimdan.linden.backup.LindenBackup
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.model.dayIn
import org.sjbtimdan.linden.ui.money.sumInDefaultMinor
import java.io.File
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Instant

private const val MIN_BACKTEST_HISTORY_MONTHS = 3

private val backupJson = Json { ignoreUnknownKeys = true }

/**
 * Manual backtest of [paceForecast] against a real Linden backup. Disabled by
 * default; run with a path to the backup zip or its extracted JSON:
 *
 * ```
 * ./gradlew :shared:jvmTest \
 *     -DpaceBacktest=/path/to/linden-backup.zip \
 *     -DpaceBacktestAsOf=2026-10-17 \
 *     -DpaceBacktestOut=/tmp/pace-backtest.txt \
 *     --tests "org.sjbtimdan.linden.ui.insights.PaceBacktestTest"
 * ```
 *
 * For every complete month and checkpoint day, the model sees only entries on
 * or before that day (its own cutoff) and predicts the month's expense total;
 * the actual is the whole month's expenses. The report prints aggregate error
 * and a per-month table. Personal data never enters the repository.
 */
class PaceBacktestTest : StringSpec({

    "pace model backtest against a Linden backup".config(
        enabled = System.getProperty("paceBacktest") != null,
    ) {
        val path = System.getProperty("paceBacktest")
        if (path == null) {
            println("SKIP: set -DpaceBacktest=<backup.zip|json> to run")
        } else {
            val file = File(path)
            if (file.isFile) {
                runBacktest(file)
            } else {
                println("SKIP: backup not found at $path")
            }
        }
    }
})

private fun runBacktest(file: File) {
    val backup = backupJson.decodeFromString<LindenBackup>(readBackupJson(file))
    val entries = toEntries(backup)
    val defaultCurrency = defaultCurrency(backup)
    val rates = toFxRates(backup)
    val zone = classificationZone(backup)
    val asOf = asOf(zone)
    val months = targetMonths(entries, asOf, zone)
    val checkpoints = checkpoints()
    val evaluatedDays = (checkpoints + 10 + 20).distinct().sorted()
    val evaluations = evaluate(entries, months, evaluatedDays, defaultCurrency, rates, zone)

    val report = StringBuilder()
    report.appendLine("=== Pace Backtest ===")
    report.appendLine("Backup: ${file.path}")
    report.appendLine("As of: $asOf")
    report.appendLine("Default currency: $defaultCurrency")
    report.appendLine("Classification zone: $zone")
    report.appendLine(
        "Entries: ${entries.size} (${entries.count { it.type == EntryType.Expense }} expenses)",
    )
    val evaluatedCount = evaluations[checkpoints.first()].orEmpty().size
    report.appendLine(
        "Target months: ${months.size} (${months.firstOrNull() ?: "-"} .. " +
            "${months.lastOrNull() ?: "-"}), evaluated: $evaluatedCount " +
            "(>= $MIN_BACKTEST_HISTORY_MONTHS months of history)",
    )
    report.appendLine("Checkpoints: ${checkpoints.joinToString(", ")}")
    report.appendLine("History window: last $PACE_HISTORY_MONTHS complete months")
    report.appendLine("Minimum history: $MIN_BACKTEST_HISTORY_MONTHS months")
    report.appendLine()
    report.appendLine("=== Accuracy by checkpoint ===")
    report.appendLine("day    n    MAPE  median  <=10%  <=20%    bias  band80  linearMAPE")
    for (day in checkpoints) {
        report.appendStats(day, evaluations[day].orEmpty())
    }
    report.appendLine()
    report.appendMonthTable("Runway at day 10", evaluations[10].orEmpty())
    report.appendLine()
    report.appendMonthTable("Runway at day 20", evaluations[20].orEmpty())

    paceForecast(entries, asOf, defaultCurrency, rates, zone)?.let { current ->
        report.appendLine()
        report.appendLine("=== Current month at $asOf ===")
        report.appendLine(
            "spent: ${money(current.spentMinor)}, projected: ${money(current.projectedMinor)} " +
                "(range ${money(current.likelyLowMinor)} .. ${money(current.likelyHighMinor)})",
        )
        report.appendLine("typical month: ${money(current.typicalMonthMinor)}")
        report.appendLine("daily allowance: ${money(current.dailyAllowanceMinor)}")
        report.appendLine(
            "history: ${current.historyMonths} months (${current.confidence})" +
                if (current.missingCurrencies.isEmpty()) {
                    ""
                } else {
                    ", missing rates: ${current.missingCurrencies.joinToString()}"
                },
        )
    }

    val text = report.toString()
    println(text)
    System.getProperty("paceBacktestOut")?.let { out -> File(out).writeText(text) }
}

private data class Evaluation(
    val month: LocalDate,
    val actual: Long,
    val projected: Long,
    val low: Long,
    val high: Long,
    val spent: Long,
    val dayOfMonth: Int,
    val daysInMonth: Int,
)

private fun evaluate(
    entries: List<Entry>,
    months: List<LocalDate>,
    checkpoints: List<Int>,
    defaultCurrency: Currency,
    rates: List<FxRate>,
    zone: TimeZone,
): Map<Int, List<Evaluation>> = checkpoints.associateWith { checkpoint ->
    months.mapNotNull { month ->
        val day = minOf(checkpoint, monthLength(month))
        val forecast = paceForecast(
            entries = entries,
            today = LocalDate(month.year, month.month.number, day),
            defaultCurrency = defaultCurrency,
            rates = rates,
            zone = zone,
        ) ?: return@mapNotNull null
        if (forecast.historyMonths < MIN_BACKTEST_HISTORY_MONTHS) return@mapNotNull null
        val actual = monthTotal(entries, month, defaultCurrency, rates, zone)
            ?: return@mapNotNull null
        if (actual == 0L) return@mapNotNull null
        Evaluation(
            month = month,
            actual = actual,
            projected = forecast.projectedMinor,
            low = forecast.likelyLowMinor,
            high = forecast.likelyHighMinor,
            spent = forecast.spentMinor,
            dayOfMonth = day,
            daysInMonth = forecast.daysInMonth,
        )
    }
}

private fun StringBuilder.appendStats(day: Int, records: List<Evaluation>) {
    if (records.isEmpty()) {
        appendLine("%3d: no data".format(Locale.US, day))
        return
    }
    val errors = records.map { errorPercent(it.projected, it.actual) }
    val signed = records.map { signedPercent(it.projected, it.actual) }
    val within10 = errors.count { it <= 0.10 } * 100.0 / errors.size
    val within20 = errors.count { it <= 0.20 } * 100.0 / errors.size
    val inBand = records.count { it.actual in it.low..it.high } * 100.0 / records.size
    val linear = records.map { record ->
        val projected = record.spent.toDouble() * record.daysInMonth / record.dayOfMonth
        errorPercent(projected, record.actual)
    }
    appendLine(
        "%3d %4d %6.1f%% %6.1f%% %5.0f%% %5.0f%% %+6.1f%% %6.0f%% %6.1f%%".format(
            Locale.US,
            day,
            records.size,
            errors.average() * 100,
            median(errors) * 100,
            within10,
            within20,
            signed.average() * 100,
            inBand,
            linear.average() * 100,
        ),
    )
}

private fun StringBuilder.appendMonthTable(title: String, records: List<Evaluation>) {
    appendLine("=== $title ===")
    if (records.isEmpty()) {
        appendLine("no data")
        return
    }
    appendLine("month          actual     predicted    error   in band")
    for (record in records) {
        val error = signedPercent(record.projected, record.actual) * 100
        val inBand = if (record.actual in record.low..record.high) "yes" else "no"
        appendLine(
            "%s %12s %12s %+7.1f%% %8s".format(
                Locale.US,
                "${record.month.year}-${record.month.month.number.toString().padStart(2, '0')}",
                money(record.actual),
                money(record.projected),
                error,
                inBand,
            ),
        )
    }
}

private fun errorPercent(projected: Long, actual: Long): Double = abs(projected - actual).toDouble() / actual

private fun errorPercent(projected: Double, actual: Long): Double = abs(projected - actual) / actual

private fun signedPercent(projected: Long, actual: Long): Double = (projected - actual).toDouble() / actual

private fun median(values: List<Double>): Double {
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 0) {
        (sorted[middle - 1] + sorted[middle]) / 2
    } else {
        sorted[middle]
    }
}

private fun money(amount: Long): String = String.format(Locale.US, "%,.0f", amount / 100.0)

private fun startOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.month.number, 1)

private fun monthLength(month: LocalDate): Int =
    startOfMonth(month).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day

private fun targetMonths(entries: List<Entry>, asOf: LocalDate, zone: TimeZone): List<LocalDate> {
    val first = entries
        .filter { it.type == EntryType.Expense }
        .map { startOfMonth(it.dayIn(zone)) }
        .minOrNull()
        ?: return emptyList()
    val last = startOfMonth(asOf).minus(1, DateTimeUnit.MONTH)
    val months = mutableListOf<LocalDate>()
    var month = first
    while (month <= last) {
        months += month
        month = month.plus(1, DateTimeUnit.MONTH)
    }
    return months
}

private fun monthTotal(
    entries: List<Entry>,
    month: LocalDate,
    defaultCurrency: Currency,
    rates: List<FxRate>,
    zone: TimeZone,
): Long? {
    val sums = mutableMapOf<Currency, Long>()
    for (entry in entries) {
        if (entry.type != EntryType.Expense) continue
        val date = entry.dayIn(zone)
        if (date.year == month.year && date.month.number == month.month.number) {
            sums.merge(entry.account.currency, entry.amount, Long::plus)
        }
    }
    return sumInDefaultMinor(sums, defaultCurrency, rates)
}

private fun checkpoints(): List<Int> = System.getProperty("paceBacktestDays")
    ?.split(',')
    ?.mapNotNull { it.trim().toIntOrNull() }
    ?.filter { it in 1..31 }
    ?.distinct()
    ?.sorted()
    ?.takeIf { it.isNotEmpty() }
    ?: listOf(5, 10, 15, 20, 25)

private fun asOf(zone: TimeZone): LocalDate {
    val requested = System.getProperty("paceBacktestAsOf")
    if (requested != null) return LocalDate.parse(requested)
    // Manual harness, like DescriptionSuggestionQualityTest: reading the wall
    // clock here is deliberate because the report is about "now".
    return Clock.System.now().toLocalDateTime(zone).date
}

private fun classificationZone(backup: LindenBackup): TimeZone {
    val requested = System.getProperty("paceBacktestZone")
    val code = requested ?: backup.entries
        .groupingBy { it.createdZone }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: "UTC"
    return runCatching { TimeZone.of(code) }.getOrDefault(TimeZone.UTC)
}

private fun defaultCurrency(backup: LindenBackup): Currency =
    backup.settings.firstOrNull { it.key == "currency" }?.value
        ?.let { runCatching { Currency.fromCode(it) }.getOrNull() }
        ?: Currency.CHF

private fun toFxRates(backup: LindenBackup): List<FxRate> = backup.fxRates.mapNotNull { rate ->
    val base = runCatching { Currency.fromCode(rate.baseCurrency) }.getOrNull()
        ?: return@mapNotNull null
    val quote = runCatching { Currency.fromCode(rate.quoteCurrency) }.getOrNull()
        ?: return@mapNotNull null
    FxRate(base, quote, rate.rate, rate.date)
}

private fun readBackupJson(file: File): String {
    if (!file.name.endsWith(".zip", ignoreCase = true)) return file.readText()
    ZipInputStream(file.inputStream().buffered()).use { zip ->
        generateSequence { zip.nextEntry }
            .firstOrNull { it.name.endsWith(".json", ignoreCase = true) }
            ?.let { return zip.readBytes().decodeToString() }
    }
    error("No JSON entry in ${file.name}")
}

private fun toEntries(backup: LindenBackup): List<Entry> {
    val accounts = backup.accounts.mapNotNull { account ->
        runCatching {
            account.id to Account(
                id = account.id,
                name = account.name,
                currency = Currency.fromCode(account.currency),
                initialBalance = account.initialBalance,
                hidden = account.hidden,
            )
        }.getOrNull()
    }.toMap()
    val categories = backup.categories.associate { category ->
        category.id to Category(
            id = category.id,
            name = category.name,
            type = runCatching { CategoryType.valueOf(category.type) }
                .getOrDefault(CategoryType.Both),
            icon = category.icon?.let { runCatching { CategoryIcon.valueOf(it) }.getOrNull() },
        )
    }
    return backup.entries.mapNotNull { entry ->
        val account = accounts[entry.accountId] ?: return@mapNotNull null
        val category = entry.categoryId?.let(categories::get)
        val createdAt = Instant.fromEpochMilliseconds(entry.createdAt)
        val createdZone = runCatching { TimeZone.of(entry.createdZone) }.getOrDefault(TimeZone.UTC)
        when (entry.type) {
            "Expense" -> category?.let {
                ExpenseEntry(
                    id = entry.id,
                    category = it,
                    description = entry.description,
                    account = account,
                    amount = entry.amount,
                    createdAt = createdAt,
                    createdZone = createdZone,
                )
            }

            "Income" -> category?.let {
                IncomeEntry(
                    id = entry.id,
                    category = it,
                    description = entry.description,
                    account = account,
                    amount = entry.amount,
                    createdAt = createdAt,
                    createdZone = createdZone,
                )
            }

            "Transfer" -> {
                val toAccount = entry.toAccountId?.let(accounts::get)
                    ?: return@mapNotNull null
                TransferEntry(
                    id = entry.id,
                    category = category,
                    description = entry.description,
                    account = account,
                    amount = entry.amount,
                    createdAt = createdAt,
                    createdZone = createdZone,
                    toAccount = toAccount,
                    toAmount = entry.toAmount,
                )
            }

            else -> null
        }
    }
}
