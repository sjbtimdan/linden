package org.sjbtimdan.linden.export

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@Composable
actual fun rememberCsvExportPicker(onPicked: (OutputStream?) -> Unit): () -> Unit = {
    val owner = Frame.getFrames().firstOrNull { it.isShowing }
    val dialog = FileDialog(owner, "Export to CSV", FileDialog.SAVE)
    dialog.file = "linden-export.csv"
    dialog.isVisible = true
    val directory = dialog.directory
    val file = dialog.file
    if (directory != null && file != null) {
        onPicked(FileOutputStream(File(directory, file)))
    } else {
        onPicked(null)
    }
}
