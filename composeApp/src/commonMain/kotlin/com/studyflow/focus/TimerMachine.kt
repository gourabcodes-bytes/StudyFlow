package com.studyflow.focus

/**
 * The focus timer as a set of pure functions. Nothing in this file touches a clock or a
 * coroutine: every function receives the current time as a parameter, which makes the
 * behaviour easy to unit test. [FocusTimer] drives it with a real clock.
 */
enum class Phase { FOCUS, BREAK }

data class TimerUiState(
    val phase: Phase = Phase.FOCUS,
    val totalSec: Int = 25 * 60,
    val remainingSec: Int = 25 * 60,
    val running: Boolean = false,
    val label: String = "",
) {
    /** Not started yet (or just reset). */
    val isFresh: Boolean get() = !running && remainingSec == totalSec

    /** 0f at the start, 1f when finished. */
    val progress: Float get() = if (totalSec <= 0) 0f else 1f - remainingSec.toFloat() / totalSec
}

/** Reported when a phase runs to zero. */
data class Completed(val phase: Phase, val startedAt: Long, val durationSec: Int, val label: String)

data class TimerData(
    val ui: TimerUiState = TimerUiState(),
    val endAtMs: Long = 0L,
    val startedAtMs: Long = 0L,
    val focusSec: Int = 25 * 60,
    val breakSec: Int = 5 * 60,
)

private fun secondsLeft(endAtMs: Long, now: Long): Int = (((endAtMs - now) + 999) / 1000).toInt()

private fun TimerData.nextPhase(): TimerData {
    val phase = if (ui.phase == Phase.FOCUS) Phase.BREAK else Phase.FOCUS
    val total = if (phase == Phase.FOCUS) focusSec else breakSec
    return copy(
        ui = TimerUiState(phase = phase, totalSec = total, remainingSec = total, running = false, label = ui.label),
        endAtMs = 0L,
        startedAtMs = 0L,
    )
}

/** New lengths apply immediately only if the timer has not been started; otherwise from the next phase. */
fun TimerData.withDurations(focusMin: Int, breakMin: Int): TimerData {
    val f = focusMin * 60
    val b = breakMin * 60
    val total = if (ui.phase == Phase.FOCUS) f else b
    val newUi = if (ui.isFresh) ui.copy(totalSec = total, remainingSec = total) else ui
    return copy(focusSec = f, breakSec = b, ui = newUi)
}

fun TimerData.withLabel(label: String): TimerData = copy(ui = ui.copy(label = label))

fun TimerData.started(now: Long): TimerData {
    if (ui.running) return this
    return copy(
        ui = ui.copy(running = true),
        endAtMs = now + ui.remainingSec * 1000L,
        startedAtMs = if (ui.isFresh) now else startedAtMs,
    )
}

/** Advance the clock. Returns [Completed] exactly once, at the moment a phase reaches zero. */
fun TimerData.ticked(now: Long): Pair<TimerData, Completed?> {
    if (!ui.running) return this to null
    val left = secondsLeft(endAtMs, now)
    if (left > 0) {
        return copy(ui = ui.copy(remainingSec = minOf(left, ui.totalSec))) to null
    }
    val done = Completed(ui.phase, startedAtMs, ui.totalSec, ui.label)
    return nextPhase() to done
}

fun TimerData.paused(now: Long): Pair<TimerData, Completed?> {
    val (afterTick, done) = ticked(now)
    if (done != null) return afterTick to done
    return afterTick.copy(ui = afterTick.ui.copy(running = false)) to null
}

/** Back to the start of the current phase, discarding progress. */
fun TimerData.reset(): TimerData {
    val total = if (ui.phase == Phase.FOCUS) focusSec else breakSec
    return copy(
        ui = ui.copy(totalSec = total, remainingSec = total, running = false),
        endAtMs = 0L,
        startedAtMs = 0L,
    )
}

/** Jump to the other phase without recording anything. */
fun TimerData.skipped(): TimerData = nextPhase()
