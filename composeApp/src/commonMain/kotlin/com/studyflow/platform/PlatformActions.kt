package com.studyflow.platform

import androidx.compose.runtime.Composable

/** Whether the app may show notifications, and a way to ask. Always granted on desktop. */
class NotificationPermission(val granted: Boolean, val request: () -> Unit)

@Composable
expect fun rememberNotificationPermission(): NotificationPermission

/**
 * Returns a function that lets the student choose where to save a text file.
 * Call it with (suggested file name, content). [onResult] says whether the file was written.
 */
@Composable
expect fun rememberFileSaver(onResult: (Boolean) -> Unit): (String, String) -> Unit

/** Returns a function that lets the student choose a text file; [onPicked] gets its text (or null). */
@Composable
expect fun rememberTextFilePicker(onPicked: (String?) -> Unit): () -> Unit
