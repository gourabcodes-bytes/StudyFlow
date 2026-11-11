package com.studyflow.ai

import androidx.compose.runtime.Composable

/** A file the student chose: its display name and its content. */
class PickedFile(val name: String, val bytes: ByteArray)

/**
 * Returns a function that opens the system file chooser for a PDF. [onPicked] receives the file,
 * or null if the student cancelled or the file could not be read.
 */
@Composable
expect fun rememberPdfPicker(onPicked: (PickedFile?) -> Unit): () -> Unit
