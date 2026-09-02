package org.sjbtimdan.linden.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Clock

@Composable
actual fun rememberDatabaseBackupPicker(onPicked: (OutputStream?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri == null) {
            onPicked(null)
        } else {
            onPicked(context.contentResolver.openOutputStream(uri))
        }
    }
    return {
        launcher.launch(backupFileName(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())))
    }
}

@Composable
actual fun rememberDatabaseRestorePicker(onPicked: (InputStream?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            onPicked(null)
        } else {
            onPicked(context.contentResolver.openInputStream(uri))
        }
    }
    return {
        launcher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
    }
}
