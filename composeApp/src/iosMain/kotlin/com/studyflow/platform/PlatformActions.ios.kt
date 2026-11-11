package com.studyflow.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberNotificationPermission(): NotificationPermission =
    remember { NotificationPermission(granted = true, request = {}) }

@Composable
actual fun rememberFileSaver(onResult: (Boolean) -> Unit): (String, String) -> Unit {
    val latestOnResult = rememberUpdatedState(onResult)
    return remember {
        { name, content ->
            latestOnResult.value(false)
        }
    }
}

@Composable
actual fun rememberTextFilePicker(onPicked: (String?) -> Unit): () -> Unit {
    val latestOnPicked = rememberUpdatedState(onPicked)
    return remember {
        {
            latestOnPicked.value(null)
        }
    }
}
