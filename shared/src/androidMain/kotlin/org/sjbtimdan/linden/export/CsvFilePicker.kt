package org.sjbtimdan.linden.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.OutputStream

@Composable
actual fun rememberCsvExportPicker(onPicked: (OutputStream?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) {
            onPicked(null)
        } else {
            onPicked(context.contentResolver.openOutputStream(uri))
        }
    }
    return {
        launcher.launch("linden-export.csv")
    }
}
