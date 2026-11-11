package com.studyflow

import com.studyflow.ai.AssistantController
import com.studyflow.data.SettingsStore
import com.studyflow.data.StudyRepository
import com.studyflow.focus.FocusTimer
import com.studyflow.focus.Phase
import com.studyflow.notify.NoNotifier
import com.studyflow.notify.Notifier
import com.studyflow.sync.SyncController
import com.studyflow.sync.SyncHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Everything the UI needs, created once per process by each platform's entry point
 * (so a running focus timer survives screen rotation and navigation).
 */
class AppContainer(
    val repository: StudyRepository,
    val settings: SettingsStore,
    scope: CoroutineScope,
    val notifier: Notifier = NoNotifier,
    /** Only the desktop app provides a hub. */
    val syncHost: SyncHost? = null,
    /** Called when a focus or break phase ends (beep / ringtone). */
    val alert: () -> Unit = {},
) {
    val sync = SyncController(repository, settings)
    val assistant = AssistantController(settings, scope)

    val timer = FocusTimer(scope) { done ->
        if (done.phase == Phase.FOCUS) {
            repository.addSession(done.label, done.startedAt, done.durationSec)
        }
        alert()
    }

    init {
        scope.launch {
            settings.loaded.first { it }
            val s = settings.state.value
            timer.setDurations(s.focusMinutes, s.breakMinutes)
        }
    }
}
