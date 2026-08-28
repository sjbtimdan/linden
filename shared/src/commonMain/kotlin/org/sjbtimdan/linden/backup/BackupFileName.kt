package org.sjbtimdan.linden.backup

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char

private val BACKUP_FILE_NAME_FORMAT = LocalDateTime.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    dayOfMonth()
    char('-')
    hour()
    minute()
    second()
}

/** Default file name for a database backup, e.g. "linden-backup-2026-08-28-153045.json". */
fun backupFileName(now: LocalDateTime): String = "linden-backup-" + BACKUP_FILE_NAME_FORMAT.format(now) + ".json"
