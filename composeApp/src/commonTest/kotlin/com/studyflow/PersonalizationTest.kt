package com.studyflow

import com.studyflow.logic.AccentSwatches
import com.studyflow.logic.accentFromSeed
import com.studyflow.logic.blendRgb
import com.studyflow.logic.brightness
import com.studyflow.logic.noteTitleFrom
import com.studyflow.logic.notePreview
import com.studyflow.logic.plannedReminders
import com.studyflow.logic.reminderFor
import com.studyflow.logic.rotateHue
import com.studyflow.model.NOTIFY_OFF
import com.studyflow.model.StudyTask
import com.studyflow.util.DAY_MS
import com.studyflow.util.HOUR_MS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersonalizationTest {

    private val now = 10 * DAY_MS

    private fun task(id: String, dueIn: Long, notify: Int = -1, done: Boolean = false, exam: Boolean = false) =
        StudyTask(id = id, title = id, dueAt = now + dueIn, notifyMinutes = notify, done = done, isExam = exam)

    // ---- reminders ----

    @Test
    fun usesTheDefaultLeadTime() {
        val r = reminderFor(task("a", 5 * HOUR_MS), enabled = true, defaultMinutes = 60, now = now)
        assertEquals(now + 4 * HOUR_MS, r?.at)
        assertTrue(r!!.text.contains("due in 1h 0m"))
    }

    @Test
    fun aDeadlineCanOverrideOrSwitchOffItsReminder() {
        val custom = reminderFor(task("a", 2 * DAY_MS, notify = 1440), true, 60, now)
        assertEquals(now + DAY_MS, custom?.at)
        assertNull(reminderFor(task("b", 2 * DAY_MS, notify = NOTIFY_OFF), true, 60, now))
        assertEquals(now + 2 * DAY_MS, reminderFor(task("c", 2 * DAY_MS, notify = 0), true, 60, now)?.at)
    }

    @Test
    fun staysQuietWhenItShould() {
        assertNull(reminderFor(task("a", 5 * HOUR_MS), enabled = false, defaultMinutes = 60, now = now), "switched off")
        assertNull(reminderFor(task("b", 5 * HOUR_MS, done = true), true, 60, now), "already done")
        assertNull(reminderFor(task("c", 30 * 60_000L), true, 60, now), "reminder time already passed")
        assertNull(reminderFor(task("d", 5 * HOUR_MS).copy(deleted = true), true, 60, now), "deleted")
    }

    @Test
    fun examsAreNamedAsExams() {
        assertEquals("Exam coming up", reminderFor(task("e", 3 * DAY_MS, exam = true), true, 60, now)?.title)
        assertEquals("Deadline coming up", reminderFor(task("h", 3 * DAY_MS), true, 60, now)?.title)
    }

    @Test
    fun plannedRemindersAreSortedAndLimited() {
        val tasks = listOf(task("late", 9 * DAY_MS), task("soon", 2 * DAY_MS), task("mid", 4 * DAY_MS))
        assertEquals(listOf("soon", "mid", "late"), plannedReminders(tasks, true, 60, now).map { it.text.substringBefore(" ·") })
        assertEquals(2, plannedReminders(tasks, true, 60, now, limit = 2).size)
        assertTrue(plannedReminders(tasks, false, 60, now).isEmpty())
    }

    // ---- colours ----

    @Test
    fun blendingAndBrightness() {
        assertEquals(0x808080, blendRgb(0x000000, 0xFFFFFF, 0.5f))
        assertEquals(0xFF0000, blendRgb(0xFF0000, 0x00FF00, 0f))
        assertEquals(0x00FF00, blendRgb(0xFF0000, 0x00FF00, 1f))
        assertEquals(0f, brightness(0x000000))
        assertEquals(1f, brightness(0xFFFFFF))
    }

    @Test
    fun hueRotation() {
        assertEquals(0x00FF00, rotateHue(0xFF0000, 120f))
        assertEquals(0x0000FF, rotateHue(0xFF0000, 240f))
        assertEquals(0xFF0000, rotateHue(0xFF0000, 360f))
        assertEquals(0x808080, rotateHue(0x808080, 90f), "greys stay grey")
    }

    @Test
    fun accentTextIsReadable() {
        // dark seeds get white text, very light seeds get dark text
        assertEquals(0xFFFFFF, accentFromSeed(0x4F46E5, dark = false).onPrimary)
        assertEquals(0x1B1B1B, accentFromSeed(0xFFFF00, dark = false).onPrimary)
        // the dark theme uses a lighter primary than the seed
        assertTrue(brightness(accentFromSeed(0x4F46E5, dark = true).primary) > brightness(0x4F46E5))
        assertEquals(0x4F46E5, accentFromSeed(0xFF4F46E5.toInt(), dark = false).primary, "alpha bits are ignored")
    }

    @Test
    fun everySwatchProducesAValidAccent() {
        for (swatch in AccentSwatches) {
            for (dark in listOf(false, true)) {
                val a = accentFromSeed(swatch, dark)
                for (c in listOf(a.primary, a.onPrimary, a.container, a.onContainer, a.secondary)) {
                    assertTrue(c in 0..0xFFFFFF)
                }
            }
        }
        assertEquals(18, AccentSwatches.distinct().size)
    }

    // ---- notes ----

    @Test
    fun notePreviewDropsMarkdown() {
        val body = "## Ohm's law\n\n**V = I × R** links voltage and current.\n- Step one\n1. Step two"
        assertEquals("Ohm's law V = I × R links voltage and current. Step one Step two", notePreview(body))
        assertEquals("abc…", notePreview("abcdef", maxChars = 3))
    }

    @Test
    fun noteTitleComesFromTheFirstLine() {
        assertEquals("Thevenin's theorem", noteTitleFrom("\n## Thevenin's theorem\nAny circuit..."))
        assertEquals("Untitled note", noteTitleFrom("   \n  "))
        assertTrue(noteTitleFrom("x".repeat(100)).endsWith("…"))
    }
}
