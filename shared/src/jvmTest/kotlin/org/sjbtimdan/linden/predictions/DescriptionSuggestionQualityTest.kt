package org.sjbtimdan.linden.predictions

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import java.io.File
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.measureTime

/**
 * Leave-one-out evaluation of description prediction quality against the real
 * local database. Not part of CI — requires a populated `~/.linden/linden.db`.
 *
 * For each sampled entry from the last year:
 *  1. Remove it from the training set (leave-one-out).
 *  2. Feed its type + category + account + amount as prediction input.
 *  3. Predict descriptions using the entry's own creation time as `now`.
 *  4. Check if the real description appears in top-N predictions.
 *
 * Run from IntelliJ: shared > jvmTest > predictions > DescriptionSuggestionQualityTest
 */
class DescriptionSuggestionQualityTest : StringSpec({

    // Skipped by default. Run manually via IntelliJ or:
    //   ./gradlew :shared:jvmTest -DdescriptionQualityTest=true \
    //       --tests "org.sjbtimdan.linden.predictions.DescriptionSuggestionQualityTest"
    "leave-one-out description prediction quality".config(
        enabled = System.getProperty("descriptionQualityTest") == "true",
    ) {
        val dbFile = File(System.getProperty("user.home"), ".linden/linden.db")
        if (dbFile.exists()) {
            val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
            val database = LindenDatabase(driver)
            val entryDao = EntryDao(database.entryQueries)
            val allEntries = entryDao.getAll().first()

            println("=== Description Suggestion Quality Evaluation ===")
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

                    var top1Hits = 0
                    var top3Hits = 0
                    var top5Hits = 0
                    var top10Hits = 0
                    var noPrediction = 0
                    val misses = mutableListOf<String>()
                    val hits = mutableListOf<String>()

                    val elapsed = measureTime {
                        for (entry in sample) {
                            val trainingData = allEntries.filter { it.id != entry.id }

                            val input = DescriptionPredictionInput(
                                type = entry.type,
                                categoryId = entry.category?.id,
                                accountId = entry.account.id,
                                amount = entry.amount,
                            )

                            val predictions = predictDescriptions(
                                entries = trainingData,
                                input = input,
                                now = entry.createdAt,
                                timeZone = tz,
                                topN = 10,
                            )

                            val actual = entry.description!!.trim()
                            if (predictions.isEmpty()) {
                                noPrediction++
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

                    println("=== BY ENTRY TYPE ===")
                    for (type in EntryType.entries) {
                        val typeSample = sample.filter { it.type == type }
                        if (typeSample.isEmpty()) continue

                        var t1 = 0; var t3 = 0; var t5 = 0; var t10 = 0; var noPred = 0
                        for (e in typeSample) {
                            val trainingData = allEntries.filter { it.id != e.id }
                            val input = DescriptionPredictionInput(
                                type = e.type,
                                categoryId = e.category?.id,
                                accountId = e.account.id,
                                amount = e.amount,
                            )
                            val preds = predictDescriptions(
                                entries = trainingData,
                                input = input,
                                now = e.createdAt,
                                timeZone = tz,
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
            driver.close()
        } else {
            println("SKIP: No local database found at ${dbFile.absolutePath}")
        }
    }
})

private fun hitPct(hits: Int, total: Int): String =
    if (total == 0) "  N/A" else "${hits * 100 / total}%"
