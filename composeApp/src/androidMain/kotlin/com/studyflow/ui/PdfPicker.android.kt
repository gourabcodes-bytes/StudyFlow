package com.studyflow.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPdfPicker(onPicked: (PickedFile?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onPicked)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) {
            latest(null)
        } else {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }
            if (bytes == null) latest(null) else latest(PickedFile(displayName(context, uri), bytes))
        }
    }

    return remember(launcher) {
        val open: () -> Unit = { launcher.launch(arrayOf("application/pdf")) }
        open
    }
}

private fun displayName(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
        } ?: "document.pdf"
    } catch (e: Exception) {
        "document.pdf"
    }
}
