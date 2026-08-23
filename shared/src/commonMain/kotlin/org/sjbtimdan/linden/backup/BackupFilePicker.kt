package org.sjbtimdan.linden.backup

import androidx.compose.runtime.Composable
import java.io.InputStream
import java.io.OutputStream

/**
 * Returns a launcher that opens the platform "save" dialog for a database backup.
 *
 * The chosen destination is passed to [onPicked] as a stream, or `null` when the
 * user cancels. The consumer owns the stream: it must be closed after use.
 */
@Composable
expect fun rememberDatabaseBackupPicker(onPicked: (OutputStream?) -> Unit): () -> Unit

/**
 * Returns a launcher that opens the platform "open" dialog for a database backup file.
 *
 * The picked file is passed to [onPicked] as an open stream, or `null` when the
 * user cancels. The consumer owns the stream: it must be closed after use.
 */
@Composable
expect fun rememberDatabaseRestorePicker(onPicked: (InputStream?) -> Unit): () -> Unit
