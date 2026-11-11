package com.studyflow.focus

import com.studyflow.util.nowMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs [TimerData] against the real clock. Because the end time is stored as a wall-clock
 * timestamp, the remaining time stays correct even if the app was paused by the OS.
 */
class FocusTimer(
    private val scope: CoroutineScope,
    private val onFinished: (Completed) -> Unit,
) {
    private val _data = MutableStateFlow(TimerData())
    val data: StateFlow<TimerData> = _data.asStateFlow()

    private var ticker: Job? = null

    private fun transition(block: (TimerData) -> Pair<TimerData, Completed?>) {
        var event: Completed? = null
        _data.update { old ->
            val (next, completed) = block(old)
            event = completed
            next
        }
        event?.let(onFinished)
    }

    fun setDurations(focusMinutes: Int, breakMinutes: Int) {
        _data.update { it.withDurations(focusMinutes, breakMinutes) }
    }

    fun setLabel(label: String) {
        _data.update { it.withLabel(label) }
    }

    fun start() {
        transition { it.started(nowMillis()) to null }
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive && _data.value.ui.running) {
                delay(200)
                transition { it.ticked(nowMillis()) }
            }
        }
    }

    fun pause() {
        transition { it.paused(nowMillis()) }
        ticker?.cancel()
        ticker = null
    }

    fun reset() {
        ticker?.cancel()
        ticker = null
        _data.update { it.reset() }
    }

    fun skip() {
        ticker?.cancel()
        ticker = null
        _data.update { it.skipped() }
    }
}
