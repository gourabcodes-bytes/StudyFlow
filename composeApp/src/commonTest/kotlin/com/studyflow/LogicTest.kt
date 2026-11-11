package com.studyflow

import com.studyflow.logic.classesOn
import com.studyflow.logic.groupActive
import com.studyflow.logic.nextClass
import com.studyflow.logic.overlaps
import com.studyflow.model.AppData
import com.studyflow.model.ClassSlot
import com.studyflow.model.StudyTask
import com.studyflow.sync.mergeData
import com.studyflow.sync.mergeLists
import com.studyflow.sync.purgeTombstones
import com.studyflow.util.DAY_MS
import com.studyflow.util.formatClock
import com.studyflow.util.formatMinute
import com.studyflow.util.humanDuration
import com.studyflow.util.normalizeHost
import com.studyflow.util.parseMinute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogicTest {

    private fun slot(id: String, day: Int, start: Int, end: Int, stamp: Long = 1L, deleted: Boolean = false) =
        ClassSlot(id = id, title = id, dayOfWeek = day, startMinute = start, endMinute = end, updatedAt = stamp, deleted = deleted)

    private fun task(id: String, due: Long, done: Boolean = false, stamp: Long = 1L) =
        StudyTask(id = id, title = id, dueAt = due, done = done, updatedAt = stamp)

    // ---- formatting / parsing ----

    @Test
    fun parseMinuteAcceptsValidTimes() {
        assertEquals(570, parseMinute("09:30"))
        assertEquals(0, parseMinute("0:00"))
        assertEquals(23 * 60 + 59, parseMinute(" 23:59 "))
    }

    @Test
    fun parseMinuteRejectsGarbage() {
        assertNull(parseMinute("24:00"))
        assertNull(parseMinute("12:60"))
        assertNull(parseMinute("noon"))
        assertNull(parseMinute("1230"))
        assertNull(parseMinute(""))
    }

    @Test
    fun formatting() {
        assertEquals("09:05", formatMinute(545))
        assertEquals("25:00", formatClock(1500))
        assertEquals("00:00", formatClock(-4))
        assertEquals("1d 1h", humanDuration(DAY_MS + 3_600_000L))
        assertEquals("1h 5m", humanDuration(3_900_000L))
        assertEquals("2m", humanDuration(-120_000L))
    }

    @Test
    fun normalizeHostAddsSchemeAndPort() {
        assertEquals("http://192.168.1.20:8765", normalizeHost("192.168.1.20", 8765))
        assertEquals("http://pc.local:9000", normalizeHost("http://pc.local:9000/", 8765))
        assertNull(normalizeHost("   ", 8765))
        assertNull(normalizeHost("not a host", 8765))
    }

    // ---- schedule ----

    @Test
    fun classesAreFilteredAndSorted() {
        val all = listOf(slot("b", 1, 600, 660), slot("a", 1, 480, 540), slot("x", 2, 480, 540), slot("gone", 1, 100, 200, deleted = true))
        assertEquals(listOf("a", "b"), classesOn(all, 1).map { it.id })
    }

    @Test
    fun overlapDetection() {
        val a = slot("a", 1, 540, 600)
        assertTrue(overlaps(a, slot("b", 1, 570, 630)))
        assertFalse(overlaps(a, slot("c", 1, 600, 660)), "touching classes do not overlap")
        assertFalse(overlaps(a, slot("d", 2, 540, 600)), "different day")
        assertFalse(overlaps(a, a), "a class never overlaps itself")
    }

    @Test
    fun nextClassIsCurrentOrUpcoming() {
        val all = listOf(slot("a", 3, 540, 600), slot("b", 3, 660, 720))
        assertEquals("a", nextClass(all, 3, 560)?.id)
        assertEquals("b", nextClass(all, 3, 610)?.id)
        assertNull(nextClass(all, 3, 800))
    }

    @Test
    fun taskGrouping() {
        val now = 10 * DAY_MS
        val groups = groupActive(
            listOf(
                task("late", now - 1000),
                task("soon", now + DAY_MS),
                task("far", now + 30 * DAY_MS),
                task("done", now + DAY_MS, done = true),
            ),
            now,
        )
        assertEquals(listOf("late"), groups.overdue.map { it.id })
        assertEquals(listOf("soon"), groups.thisWeek.map { it.id })
        assertEquals(listOf("far"), groups.later.map { it.id })
    }

    // ---- sync merge ----

    @Test
    fun newerCopyWins() {
        val local = listOf(task("t", 1, stamp = 5))
        val remote = listOf(task("t", 2, stamp = 9))
        assertEquals(2L, mergeLists(local, remote).single().dueAt)
        assertEquals(2L, mergeLists(remote, local).single().dueAt)
    }

    @Test
    fun tieKeepsLocal() {
        val merged = mergeLists(listOf(task("t", 1, stamp = 5)), listOf(task("t", 2, stamp = 5)))
        assertEquals(1L, merged.single().dueAt)
    }

    @Test
    fun itemsFromBothSidesSurvive() {
        val merged = mergeLists(listOf(task("a", 1)), listOf(task("b", 2)))
        assertEquals(setOf("a", "b"), merged.map { it.id }.toSet())
    }

    @Test
    fun deletionTravelsViaTombstone() {
        val local = listOf(slot("a", 1, 540, 600, stamp = 5))
        val remote = listOf(slot("a", 1, 540, 600, stamp = 8, deleted = true))
        val merged = mergeLists(local, remote)
        assertTrue(merged.single().deleted)
    }

    @Test
    fun mergeIsIdempotent() {
        val a = AppData(tasks = listOf(task("a", 1, stamp = 3)), classes = listOf(slot("c", 1, 1, 2, stamp = 4)))
        val b = AppData(tasks = listOf(task("a", 2, stamp = 7), task("b", 5, stamp = 1)))
        val once = mergeData(a, b)
        assertEquals(once, mergeData(once, b))
        assertEquals(once, mergeData(once, a))
    }

    @Test
    fun oldTombstonesArePurged() {
        val data = AppData(classes = listOf(slot("old", 1, 1, 2, stamp = 10, deleted = true), slot("new", 1, 1, 2, stamp = 100, deleted = true)))
        assertEquals(listOf("new"), data.purgeTombstones(cutoff = 50).classes.map { it.id })
    }
}
