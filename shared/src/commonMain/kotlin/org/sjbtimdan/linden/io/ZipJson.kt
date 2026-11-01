package org.sjbtimdan.linden.io

import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Reads the first `.json` entry from [input] and decodes its bytes with [decode].
 * Returns null when the archive has no JSON entry. I/O and decode failures
 * propagate to the caller, which owns the feature-specific error message.
 */
internal fun readFirstJsonEntry(input: InputStream, decode: (ByteArray) -> String): String? {
    val zip = ZipInputStream(BufferedInputStream(input))
    zip.use { zis ->
        generateSequence { zis.nextEntry }
            .firstOrNull { it.name.endsWith(".json") }
            ?: return null
        return decode(zis.readBytes())
    }
}
