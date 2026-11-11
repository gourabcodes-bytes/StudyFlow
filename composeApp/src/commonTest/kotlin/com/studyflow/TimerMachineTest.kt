package com.studyflow

import com.studyflow.focus.Phase
import com.studyflow.focus.TimerData
import com.studyflow.focus.paused
import com.studyflow.focus.reset
import com.studyflow.focus.skipped
import com.studyflow.focus.started
import com.studyflow.focus.ticked
import com.studyflow.focus.withDurations
import com.studyflow.focus.withLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimerMachineTest {

    @Test
    fun countsDownWhileRunning() {
        val t = TimerData().started(now = 0)
        val (after, done) = t.ticked(now = 10_000)
        assertNull(done)
        assertEquals(25 * 60 - 10, after.ui.remainingSec)
        assertTrue(after.ui.running)
    }

    @Test
    fun pauseFreezesAndResumeContinues() {
        val running = TimerData().started(now = 0)
        val (paused, _) = running.paused(now = 20_000)
        assertFalse(paused.ui.running)
        assertEquals(25 * 60 - 20, paused.ui.remainingSec)

        // time passing while paused changes nothing
        val (still, _) = paused.ticked(now = 500_000)
        assertEquals(paused.ui.remainingSec, still.ui.remainingSec)

        val resumed = paused.started(now = 600_000)
        val (later, _) = resumed.ticked(now = 610_000)
        assertEquals(25 * 60 - 30, later.ui.remainingSec)
    }

    @Test
    fun completingFocusReportsOnceAndStartsBreak() {
        val t = TimerData().withLabel("Maths").started(now = 1_000)
        val (next, done) = t.ticked(now = 1_000 + 25 * 60 * 1000L)
        assertEquals(Phase.FOCUS, done?.phase)
        assertEquals(25 * 60, done?.durationSec)
        assertEquals(1_000L, done?.startedAt)
        assertEquals("Maths", done?.label)
        assertEquals(Phase.BREAK, next.ui.phase)
        assertEquals(5 * 60, next.ui.remainingSec)
        assertFalse(next.ui.running)
        // ticking again does not report a second completion
        assertNull(next.ticked(now = 9_999_999).second)
    }

    @Test
    fun startedAtSurvivesPauseResume() {
        val t = TimerData().started(now = 5_000)
        val (p, _) = t.paused(now = 15_000)
        val r = p.started(now = 100_000)
        val (_, done) = r.ticked(now = 100_000 + (25 * 60 - 10) * 1000L)
        assertEquals(5_000L, done?.startedAt)
    }

    @Test
    fun durationsApplyOnlyWhenFresh() {
        val fresh = TimerData().withDurations(45, 10)
        assertEquals(45 * 60, fresh.ui.remainingSec)
        assertEquals(45 * 60, fresh.ui.totalSec)

        val (midway, _) = TimerData().started(now = 0).ticked(now = 60_000)
        val changed = midway.withDurations(15, 5)
        assertEquals(midway.ui.totalSec, changed.ui.totalSec, "running session keeps its length")
        assertEquals(15 * 60, changed.focusSec)
        // ...but the next phase uses the new lengths
        val finished = changed.copy(endAtMs = 0).started(now = 0).ticked(now = 100_000_000).first
        assertEquals(5 * 60, finished.ui.totalSec)
    }

    @Test
    fun resetAndSkip() {
        val (midway, _) = TimerData().started(now = 0).ticked(now = 90_000)
        val r = midway.reset()
        assertTrue(r.ui.isFresh)
        assertEquals(25 * 60, r.ui.remainingSec)

        val s = r.skipped()
        assertEquals(Phase.BREAK, s.ui.phase)
        assertEquals(Phase.FOCUS, s.skipped().ui.phase)
    }

    @Test
    fun progressGoesFromZeroToOne() {
        val t = TimerData().withDurations(1, 1)
        assertEquals(0f, t.ui.progress)
        val (half, _) = t.started(0).ticked(30_000)
        assertEquals(0.5f, half.ui.progress)
    }
}
