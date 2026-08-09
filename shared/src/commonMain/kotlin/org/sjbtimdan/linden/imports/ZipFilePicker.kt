package org.sjbtimdan.linden.imports

import androidx.compose.runtime.Composable
import java.io.InputStream

/**
 * Returns a launcher that opens the platform file picker for an Ivy backup zip.
 *
 * The picked file is passed to [onPicked] as an open stream, or `null` when the
 * user cancels. The consumer owns the stream: it must be closed after use.
 */
@Composable
expect fun rememberZipFilePicker(onPicked: (InputStream?) -> Unit): () -> Unit
