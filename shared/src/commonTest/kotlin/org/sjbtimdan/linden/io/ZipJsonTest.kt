package org.sjbtimdan.linden.io

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipJsonTest : StringSpec({
    "reads the first JSON entry and ignores other entries" {
        val bytes = zipOf(
            "notes.txt" to "ignored".encodeToByteArray(),
            "first.json" to """{"source":"first"}""".encodeToByteArray(),
            "second.json" to """{"source":"second"}""".encodeToByteArray(),
        )

        readFirstJsonEntry(ByteArrayInputStream(bytes)) { it.decodeToString() } shouldBe
            """{"source":"first"}"""
    }

    "returns null when the archive has no JSON entry" {
        val bytes = zipOf("notes.txt" to "ignored".encodeToByteArray())

        readFirstJsonEntry(ByteArrayInputStream(bytes)) { it.decodeToString() } shouldBe null
    }

    "uses the caller's decoder for non-UTF-8 content" {
        val json = """{"name":"Café"}"""
        val bytes = zipOf("backup.json" to json.toByteArray(Charsets.UTF_16BE))

        readFirstJsonEntry(ByteArrayInputStream(bytes)) { it.toString(Charsets.UTF_16BE) } shouldBe json
    }
})

private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        entries.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return output.toByteArray()
}
