package org.sjbtimdan.linden.imports

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

@Composable
actual fun rememberZipFilePicker(onPicked: (InputStream?) -> Unit): () -> Unit = {
    val owner = Frame.getFrames().firstOrNull { it.isShowing }
    val dialog = FileDialog(owner, "Import from Ivy", FileDialog.LOAD)
    dialog.isVisible = true
    val directory = dialog.directory
    val file = dialog.file
    if (directory != null && file != null) {
        onPicked(FileInputStream(File(directory, file)))
    } else {
        onPicked(null)
    }
}
