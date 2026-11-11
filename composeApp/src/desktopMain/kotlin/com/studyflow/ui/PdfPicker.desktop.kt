package com.studyflow.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberPdfPicker(onPicked: (PickedFile?) -> Unit): () -> Unit {
    val latestOnPicked = rememberUpdatedState(onPicked)
    return remember {
        {
            val dialog = FileDialog(null as Frame?, "Select PDF", FileDialog.LOAD)
            dialog.file = "*.pdf"
            dialog.isVisible = true
            val file = dialog.file
            val dir = dialog.directory
            if (file != null && dir != null) {
                val pdfFile = File(dir, file)
                val bytes = try { pdfFile.readBytes() } catch (e: Exception) { null }
                if (bytes != null) {
                    latestOnPicked.value(PickedFile(pdfFile.name, bytes))
                } else {
                    latestOnPicked.value(null)
                }
            } else {
                latestOnPicked.value(null)
            }
        }
    }
}
