package com.studyflow

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.studyflow.data.SettingsStore
import com.studyflow.data.StudyRepository
import com.studyflow.data.createStorage
import com.studyflow.notify.DesktopNotifier
import com.studyflow.sync.DesktopSyncHost
import com.studyflow.ui.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import java.awt.Toolkit

fun main() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val storage = createStorage()
    val repository = StudyRepository(storage, scope)
    val settings = SettingsStore(storage, scope)
    val host = DesktopSyncHost(repository, settings)
    val notifier = DesktopNotifier(scope)

    val container = AppContainer(
        repository = repository,
        settings = settings,
        scope = scope,
        notifier = notifier,
        syncHost = host,
        alert = { Toolkit.getDefaultToolkit().beep() },
    )

    application {
        Window(
            onCloseRequest = {
                host.stop()
                // make sure the very last change is on disk before the process exits
                runBlocking {
                    repository.flush()
                    settings.flush()
                }
                exitApplication()
            },
            title = "StudyFlow",
            state = rememberWindowState(width = 1100.dp, height = 760.dp),
        ) {
            App(container)
        }
    }
}
