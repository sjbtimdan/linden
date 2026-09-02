package org.sjbtimdan.linden.backup

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Builds a zipped Linden backup containing the given JSON payload. */
internal fun buildBackupZip(json: String): ByteArray {
    val bytes = ByteArrayOutputStream()
    ZipOutputStream(bytes).use { zip ->
        zip.putNextEntry(ZipEntry("linden-backup.json"))
        zip.write(json.encodeToByteArray())
        zip.closeEntry()
    }
    return bytes.toByteArray()
}
