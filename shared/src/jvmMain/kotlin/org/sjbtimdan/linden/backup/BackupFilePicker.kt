package org.sjbtimdan.linden.backup

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

@Composable
actual fun rememberDatabaseBackupPicker(onPicked: (OutputStream?) -> Unit): () -> Unit = {
    val owner = Frame.getFrames().firstOrNull { it.isShowing }
    val dialog = FileDialog(owner, "Back up database", FileDialog.SAVE)
    dialog.isVisible = true
    val directory = dialog.directory
    val file = dialog.file
    if (directory != null && file != null) {
        onPicked(FileOutputStream(File(directory, file)))
    } else {
        onPicked(null)
    }
}

@Composable
actual fun rememberDatabaseRestorePicker(onPicked: (InputStream?) -> Unit): () -> Unit = {
    val owner = Frame.getFrames().firstOrNull { it.isShowing }
    val dialog = FileDialog(owner, "Restore from backup", FileDialog.LOAD)
    dialog.isVisible = true
    val directory = dialog.directory
    val file = dialog.file
    if (directory != null && file != null) {
        onPicked(FileInputStream(File(directory, file)))
    } else {
        onPicked(null)
    }
}
