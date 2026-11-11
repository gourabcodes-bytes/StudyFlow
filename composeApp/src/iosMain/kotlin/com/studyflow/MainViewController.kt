package com.studyflow

import androidx.compose.ui.window.ComposeUIViewController
import com.studyflow.data.SettingsStore
import com.studyflow.data.StudyRepository
import com.studyflow.data.createStorage
import com.studyflow.ui.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val storage = createStorage()
    val container = AppContainer(
        repository = StudyRepository(storage, scope),
        settings = SettingsStore(storage, scope),
        scope = scope,
    )
    return ComposeUIViewController {
        App(container = container)
    }
}