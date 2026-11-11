package com.studyflow

import androidx.compose.ui.window.ComposeUIViewController
import com.studyflow.ui.App

fun MainViewController(appContainer: AppContainer) = ComposeUIViewController {
    App(container = appContainer)
}
