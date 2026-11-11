package com.studyflow.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberPdfPicker(onPicked: (PickedFile?) -> Unit): () -> Unit {
    val latestOnPicked = rememberUpdatedState(onPicked)
    return remember {
        {
            latestOnPicked.value(null)
        }
    }
}
