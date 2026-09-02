package org.sjbtimdan.linden.export

import androidx.compose.runtime.Composable
import java.io.OutputStream

/**
 * Returns a launcher that opens the platform "save" dialog for a CSV export.
 *
 * The chosen destination is passed to [onPicked] as a stream, or `null` when the
 * user cancels. The consumer owns the stream: it must be closed after use.
 */
@Composable
expect fun rememberCsvExportPicker(onPicked: (OutputStream?) -> Unit): () -> Unit
