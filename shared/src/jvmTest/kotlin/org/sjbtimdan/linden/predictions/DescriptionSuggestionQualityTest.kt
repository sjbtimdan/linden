package org.sjbtimdan.linden.predictions

import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import org.sjbtimdan.linden.backup.LindenBackupManager
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import java.io.File
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.measureTime

/**
 * Leave-one-out evaluation of description prediction quality against the backup
 * in the repo's `integrationTestData/` directory, restored into a fresh in-memory
 * database. Not part of CI.
 *
 * For each sampled entry from the last year:
 *  1. Remove it and every entry created after it from the training set
 *     (leave-one-out, no lookahead).
 *  2. Feed its type + category + account + amount as prediction input.
 *  3. Predict descriptions using the entry's own creation time as `now`.
 *  4. Check if the real description appears in top-N predictions.
 *
 * Reports the cold hit rates (no typed text), the share of targets with an
 * eligible past occurrence (the ranking ceiling), and a warm pass that types
 * the first [WARM_PREFIX_LENGTH] characters of each description.
 *
 * Run from IntelliJ: run the test method below from the gutter (a class-level
 * run skips the test because it is disabled by default).
 */
class DescriptionSuggestionQualityTest : StringSpec({

    // Skipped by default. Run manually from the IDE (test method, not the class) or:
    //   ./gradlew :shared:jvmTest -DdescriptionQualityTest=true \
    //       --tests "org.sjbtimdan.linden.predictions.DescriptionSuggestionQualityTest"
    "leave-one-out description prediction quality".config(
        enabled = System.getProperty("descriptionQualityTest") == "true",
    ) {
        val backupZip = integrationBackupZip()
        if (backupZip != null) {
            val database = lindenDatabase()
            val restored = LindenBackupManager(database).restoreFrom(backupZip.inputStream())
            val entryDao = EntryDao(database.entryQueries)
            val allEntries = entryDao.getAll().first()

            println("=== Description Suggestion Quality Evaluation ===")
            println(
                "Restored integration backup: ${restored.accounts} accounts, " +
                    "${restored.categories} categories, ${restored.entries} entries",
            )
            println("Total entries in database: ${allEntries.size}")

            if (allEntries.isNotEmpty()) {
                val tz = TimeZone.currentSystemDefault()
                val now = Clock.System.now()
                val oneYearAgo = now.minus(12, DateTimeUnit.MONTH, tz)

                val testable = allEntries.filter { entry ->
                    entry.createdAt >= oneYearAgo &&
                        entry.description?.trim()?.isNotEmpty() == true
                }
                println("Entries in last year with description: ${testable.size}")

                if (testable.isNotEmpty()) {
                    val sampleSize = minOf(300, testable.size)
                    val sample = testable.shuffled(Random(42)).take(sampleSize)
                    println("Sample size: $sampleSize")
                    println()

                    println("Sample by type:")
                    sample.groupBy { it.type }.forEach { (type, entries) ->
                        println("  ${type.name}: ${entries.size}")
                    }
                    println()

                    val reachable = sample.associate { entry ->
                        entry.id to hasEligibleOccurrence(allEntries, entry)
                    }
                    val reachableCount = reachable.values.count { it }
                    println(
                        "Targets with an eligible past occurrence (ranking ceiling): " +
                            "$reachableCount / $sampleSize (${reachableCount * 100 / sampleSize}%)",
                    )
                    println()

                    var top1Hits = 0
                    var top3Hits = 0
                    var top5Hits = 0
                    var top10Hits = 0
                    var noPrediction = 0
                    var unreachableMisses = 0
                    val misses = mutableListOf<String>()
                    val hits = mutableListOf<String>()

                    val elapsed = measureTime {
                        for (entry in sample) {
                            val trainingData = pastTrainingData(allEntries, entry)

                            val input = DescriptionPredictionInput(
                                type = entry.type,
                                categoryId = entry.category?.id,
                                accountId = entry.account.id,
                                amount = entry.amount,
                                description = null,
                            )

                            val predictions = predictDescriptions(
                                entries = trainingData,
                                input = input,
                                now = entry.createdAt,
                                topN = 10,
                            )

                            val actual = entry.description!!.trim()
                            if (predictions.isEmpty()) {
                                noPrediction++
                                if (reachable[entry.id] == false) unreachableMisses++
                            } else {
                                val predictedLower = predictions.map { it.lowercase() }
                                val actualLower = actual.lowercase()
                                val rank = predictedLower.indexOf(actualLower)

                                if (rank >= 0) {
                                    val r = rank + 1
                                    if (r <= 1) top1Hits++
                                    if (r <= 3) top3Hits++
                                    if (r <= 5) top5Hits++
                                    if (r <= 10) top10Hits++

                                    if (hits.size < 15) {
                                        hits.add(
                                            "${entry.type.name.padEnd(8)} " +
                                                "\"${actual.take(25)}\"".padEnd(28) +
                                                "rank=$r " +
                                                "cat=${entry.category?.name?.take(12) ?: "—"} " +
                                                "amt=${entry.amount}",
                                        )
                                    }
                                } else {
                                    if (reachable[entry.id] == false) unreachableMisses++
                                    if (misses.size < 25) {
                                        misses.add(
                                            "${entry.type.name.padEnd(8)} " +
                                                "\"${actual.take(25)}\"".padEnd(28) +
                                                "cat=${entry.category?.name?.take(12) ?: "—"} " +
                                                "amt=${entry.amount} " +
                                                "→ top3=[${predictions.take(3).joinToString { "\"${it.take(15)}\"" }}]",
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val withPredictions = sampleSize - noPrediction
                    println("=== RESULTS (${elapsed.inWholeMilliseconds}ms) ===")
                    println()
                    println(
                        "Entries with predictions: $withPredictions / $sampleSize " +
                            "(${withPredictions * 100 / sampleSize}%)",
                    )
                    println(
                        "No predictions possible:  $noPrediction / $sampleSize " +
                            "(${noPrediction * 100 / sampleSize}%)",
                    )
                    println()
                    println("Hit rates (of entries where predictor returned suggestions):")
                    println(
                        "  Top-1:  ${top1Hits.toString().padStart(4)} / $withPredictions " +
                            "(${hitPct(top1Hits, withPredictions)})",
                    )
                    println(
                        "  Top-3:  ${top3Hits.toString().padStart(4)} / $withPredictions " +
                            "(${hitPct(top3Hits, withPredictions)})",
                    )
                    println(
                        "  Top-5:  ${top5Hits.toString().padStart(4)} / $withPredictions " +
                            "(${hitPct(top5Hits, withPredictions)})",
                    )
                    println(
                        "  Top-10: ${top10Hits.toString().padStart(4)} / $withPredictions " +
                            "(${hitPct(top10Hits, withPredictions)})",
                    )
                    println()
                    val totalMisses = sampleSize - noPrediction - top10Hits
                    println(
                        "Misses: $totalMisses — $unreachableMisses unreachable (no eligible past occurrence), " +
                            "${totalMisses - unreachableMisses} ranked outside top 10",
                    )
                    println()

                    println("=== BY ENTRY TYPE ===")
                    for (type in EntryType.entries) {
                        val typeSample = sample.filter { it.type == type }
                        if (typeSample.isEmpty()) continue

                        var t1 = 0
                        var t3 = 0
                        var t5 = 0
                        var t10 = 0
                        var noPred = 0
                        for (e in typeSample) {
                            val trainingData = pastTrainingData(allEntries, e)
                            val input = DescriptionPredictionInput(
                                type = e.type,
                                categoryId = e.category?.id,
                                accountId = e.account.id,
                                amount = e.amount,
                                description = null,
                            )
                            val preds = predictDescriptions(
                                entries = trainingData,
                                input = input,
                                now = e.createdAt,
                                topN = 10,
                            )
                            if (preds.isEmpty()) {
                                noPred++
                            } else {
                                val idx = preds.map { it.lowercase() }
                                    .indexOf(e.description!!.trim().lowercase())
                                if (idx >= 0) {
                                    val r = idx + 1
                                    if (r <= 1) t1++
                                    if (r <= 3) t3++
                                    if (r <= 5) t5++
                                    if (r <= 10) t10++
                                }
                            }
                        }
                        val tp = typeSample.size - noPred
                        println(
                            "  ${type.name.padEnd(10)} n=${typeSample.size.toString().padStart(4)}  " +
                                "top1=${hitPct(t1, tp).padStart(5)}  " +
                                "top3=${hitPct(t3, tp).padStart(5)}  " +
                                "top5=${hitPct(t5, tp).padStart(5)}  " +
                                "top10=${hitPct(t10, tp).padStart(5)}",
                        )
                    }
                    println()

                    var warmTop1 = 0
                    var warmTop3 = 0
                    var warmTop5 = 0
                    var warmTop10 = 0
                    var warmNoPrediction = 0
                    for (entry in sample) {
                        val actual = entry.description!!.trim()
                        val predictions = predictDescriptions(
                            entries = pastTrainingData(allEntries, entry),
                            input = DescriptionPredictionInput(
                                type = entry.type,
                                categoryId = entry.category?.id,
                                accountId = entry.account.id,
                                amount = entry.amount,
                                description = actual.take(WARM_PREFIX_LENGTH),
                            ),
                            now = entry.createdAt,
                            topN = 10,
                        )
                        if (predictions.isEmpty()) {
                            warmNoPrediction++
                        } else {
                            val rank = predictions.map { it.lowercase() }.indexOf(actual.lowercase())
                            if (rank >= 0) {
                                val r = rank + 1
                                if (r <= 1) warmTop1++
                                if (r <= 3) warmTop3++
                                if (r <= 5) warmTop5++
                                if (r <= 10) warmTop10++
                            }
                        }
                    }
                    val warmWithPredictions = sampleSize - warmNoPrediction
                    println("=== WARM (first $WARM_PREFIX_LENGTH typed characters) ===")
                    println(
                        "Entries with predictions: $warmWithPredictions / $sampleSize " +
                            "(${warmWithPredictions * 100 / sampleSize}%)",
                    )
                    println("Hit rates (of entries where predictor returned suggestions):")
                    val warmRates = listOf(
                        "Top-1" to warmTop1,
                        "Top-3" to warmTop3,
                        "Top-5" to warmTop5,
                        "Top-10" to warmTop10,
                    )
                    for ((label, count) in warmRates) {
                        println(
                            "  $label:  ${count.toString().padStart(4)} / $warmWithPredictions " +
                                "(${hitPct(count, warmWithPredictions)})",
                        )
                    }
                    println()

                    if (hits.isNotEmpty()) {
                        println("=== SAMPLE HITS (${hits.size}) ===")
                        hits.forEach { println("  $it") }
                        println()
                    }

                    if (misses.isNotEmpty()) {
                        println("=== SAMPLE MISSES (${misses.size} shown) ===")
                        misses.forEach { println("  $it") }
                        println()
                    }
                } else {
                    println("SKIP: No testable entries found")
                }
            } else {
                println("SKIP: Database is empty")
            }
        } else {
            println(
                "SKIP: No backup zip found in integrationTestData/ " +
                    "(looked in ../integrationTestData and integrationTestData)",
            )
        }
    }
})

private fun hitPct(hits: Int, total: Int): String = if (total == 0) "  N/A" else "${hits * 100 / total}%"

/** Characters typed in the warm evaluation pass. */
private const val WARM_PREFIX_LENGTH = 3

/** Training data for [entry]: every entry but itself, nothing created after it. */
private fun pastTrainingData(allEntries: List<Entry>, entry: Entry): List<Entry> =
    allEntries.filter { it.id != entry.id && it.createdAt < entry.createdAt }

/**
 * Whether the cold predictor rules can surface [entry]'s own description: same
 * type, category-constrained (with fallback) and at least one field match.
 */
private fun hasEligibleOccurrence(allEntries: List<Entry>, entry: Entry): Boolean {
    val trainingData = pastTrainingData(allEntries, entry)
    val sameType = trainingData.filter { it.type == entry.type }
    val category = entry.category
    val candidates = if (category != null) {
        sameType.filter { it.category?.id == category.id }.ifEmpty { sameType }
    } else {
        sameType
    }
    val actual = entry.description!!.trim()
    return candidates.any { candidate ->
        candidate.description?.trim().equals(actual, ignoreCase = true) &&
            baseMatchScore(candidate, entry.category?.id, entry.account.id, entry.amount) != null
    }
}

/** Locates the backup zip in the repo's `integrationTestData/` directory. */
private fun integrationBackupZip(): File? {
    val dir = listOf(File("../integrationTestData"), File("integrationTestData"))
        .firstOrNull { it.isDirectory } ?: return null
    return dir.listFiles { file -> file.isFile && file.extension == "zip" }?.firstOrNull()
}
