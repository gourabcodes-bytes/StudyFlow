package com.studyflow.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberNotificationPermission(): NotificationPermission =
    NotificationPermission(granted = true, request = {})

@Composable
actual fun rememberFileSaver(onResult: (Boolean) -> Unit): (String, String) -> Unit {
    val latest by rememberUpdatedState(onResult)
    return remember {
        val save: (String, String) -> Unit = { name, content ->
            val dialog = FileDialog(null as Frame?, "Save backup", FileDialog.SAVE)
            dialog.file = name
            dialog.isVisible = true // waits here until the student chooses or cancels
            val directory = dialog.directory
            val chosen = dialog.file
            if (directory == null || chosen == null) {
                latest(false)
            } else {
                val written = try {
                    File(directory, chosen).writeText(content, Charsets.UTF_8)
                    true
                } catch (e: Exception) {
                    false
                }
                latest(written)
            }
        }
        save
    }
}

@Composable
actual fun rememberTextFilePicker(onPicked: (String?) -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onPicked)
    return remember {
        val open: () -> Unit = {
            val dialog = FileDialog(null as Frame?, "Choose a backup", FileDialog.LOAD)
            dialog.setFilenameFilter { _, name -> name.lowercase().endsWith(".json") }
            dialog.isVisible = true
            val directory = dialog.directory
            val chosen = dialog.file
            if (directory == null || chosen == null) {
                latest(null)
            } else {
                val text = try {
                    File(directory, chosen).readText(Charsets.UTF_8)
                } catch (e: Exception) {
                    null
                }
                latest(text)
            }
        }
        open
    }
}
