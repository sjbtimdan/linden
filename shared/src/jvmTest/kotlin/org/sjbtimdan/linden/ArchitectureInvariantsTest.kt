package org.sjbtimdan.linden

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File

/**
 * Rules from `AGENTS.md`/`living-notes.md` that would otherwise be prose only. These
 * are text scans, not compiler checks: they catch the obvious regression, not a clever
 * one. They live in jvmTest because they read the source tree.
 */
class ArchitectureInvariantsTest : StringSpec({

    // Gradle runs tests with the module as working directory; the IDE with the repo root.
    val shared = sequenceOf(File("src"), File("shared/src"))
        .firstOrNull { File(it, "commonMain/kotlin").isDirectory }
        ?: error("Cannot locate :shared sources from ${File(".").absolutePath}")

    fun sources(vararg sourceSets: String): List<File> = sourceSets
        .flatMap { File(shared, "$it/kotlin").walkTopDown().toList() }
        .filter { it.extension == "kt" }

    /** `path:line: code` for every line matching [pattern], ignoring comments. */
    fun violations(file: File, pattern: Regex): List<String> = file.readLines()
        .mapIndexedNotNull { index, line ->
            val code = line.substringBefore("//").trim()
            val isComment = code.startsWith("*") || code.startsWith("/*")
            if (!isComment && pattern.containsMatchIn(code)) "${file.path}:${index + 1}: $code" else null
        }

    "the scans see the sources they police" {
        // A text scan that silently matches nothing always passes. Fail loudly if the
        // source layout moves, so these rules cannot quietly stop being enforced.
        sources("commonMain").size shouldBeGreaterThan 100
        sources("commonMain").count { it.parentFile.name == "model" } shouldBeGreaterThan 5
        File(shared, "commonMain/sqldelight").walkTopDown().count { it.extension == "sq" } shouldBeGreaterThan 3

        // The exemption below is by filename; if the production clock disappears the
        // rule is vacuous, so require it to exist and to be the real implementation.
        val systemClock = File(shared, "commonMain/kotlin/org/sjbtimdan/linden/time/AppClock.kt")
        systemClock.isFile shouldBe true
        systemClock.readText() shouldContain "Clock.System.now()"
    }

    "app code reads time only through AppClock" {
        val offenders = sources("commonMain", "androidMain", "jvmMain", "jvmSharedMain")
            .filterNot { it.name == "AppClock.kt" }
            .flatMap { violations(it, Regex("""Clock\.System\.""")) }

        offenders.shouldBeEmpty()
    }

    "the domain model never stores money as floating point" {
        val offenders = sources("commonMain")
            .filter { it.parentFile.name == "model" }
            // FX rates are not money; they are the documented exception in living-notes.md.
            .filterNot { it.name == "FxRate.kt" }
            .flatMap { violations(it, Regex(""":\s*(Double|Float|BigDecimal)\b""")) }

        offenders.shouldBeEmpty()
    }

    "the schema never stores money as REAL" {
        val files = File(shared, "commonMain/sqldelight").walkTopDown()
            .filter { it.extension == "sq" && it.name != "FxRate.sq" }
            .toList()

        files.flatMap { violations(it, Regex("""\bREAL\b""")) }.shouldBeEmpty()
    }
})
