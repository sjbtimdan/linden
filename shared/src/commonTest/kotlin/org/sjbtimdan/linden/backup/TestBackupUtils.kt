package org.sjbtimdan.linden.backup

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Zips [json] as the single [entryName] file, encoded with [charset]. */
internal fun buildJsonZip(json: String, entryName: String, charset: Charset = Charsets.UTF_8): ByteArray {
    val bytes = ByteArrayOutputStream()
    ZipOutputStream(bytes).use { zip ->
        zip.putNextEntry(ZipEntry(entryName))
        zip.write(json.toByteArray(charset))
        zip.closeEntry()
    }
    return bytes.toByteArray()
}

/** Builds a zipped Linden backup containing the given JSON payload. */
internal fun buildBackupZip(json: String): ByteArray = buildJsonZip(json, "linden-backup.json")
