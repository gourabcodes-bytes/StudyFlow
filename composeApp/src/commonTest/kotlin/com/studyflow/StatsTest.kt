package com.studyflow

import com.studyflow.logic.dailyProgress
import com.studyflow.logic.focusStreak
import com.studyflow.logic.upcomingExams
import com.studyflow.logic.weekProgress
import com.studyflow.model.ClassSlot
import com.studyflow.model.StudyTask
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTest {

    private fun task(id: String, due: Long, done: Boolean = false, deleted: Boolean = false) =
        StudyTask(id = id, title = id, dueAt = due, done = done, deleted = deleted)

    @Test
    fun streakCountsConsecutiveDays() {
        assertEquals(3, focusStreak(setOf(8L, 9L, 10L), today = 10L))
    }

    @Test
    fun streakSurvivesUntilEndOfToday() {
        // nothing yet today, but yesterday and the day before count
        assertEquals(2, focusStreak(setOf(8L, 9L), today = 10L))
    }

    @Test
    fun streakBreaksOnAGap() {
        assertEquals(1, focusStreak(setOf(7L, 10L), today = 10L))
        assertEquals(0, focusStreak(setOf(5L, 6L), today = 10L))
        assertEquals(0, focusStreak(emptySet(), today = 10L))
    }

    @Test
    fun weekProgressCountsOnlyThisWeek() {
        val start = 1_000L
        val end = 2_000L
        val now = 1_500L
        val tasks = listOf(
            task("done", 1_200L, done = true),
            task("late", 1_100L),
            task("later", 1_900L),
            task("lastWeek", 500L),
            task("nextWeek", 2_500L),
            task("deleted", 1_300L, deleted = true),
        )
        val p = weekProgress(tasks, start, end, now)
        assertEquals(3, p.total)
        assertEquals(1, p.done)
        assertEquals(1, p.overdue)
        assertEquals(1f / 3f, p.fraction)
    }

    @Test
    fun emptyWeekHasZeroFraction() {
        assertEquals(0f, weekProgress(emptyList(), 0L, 10L, 5L).fraction)
    }

    private fun slot(id: String, start: Int, end: Int) =
        ClassSlot(id = id, title = id, dayOfWeek = 2, startMinute = start, endMinute = end)

    @Test
    fun dailyProgressAveragesWhatIsPlanned() {
        // 2 of 4 classes over, no deadlines today, 60 of 120 focus minutes -> (0.5 + 0.5) / 2
        val classes = listOf(slot("a", 480, 540), slot("b", 540, 600), slot("c", 780, 840), slot("d", 840, 900))
        val p = dailyProgress(classes, dayIso = 2, nowMinute = 700, tasks = emptyList(), dayStart = 0, dayEnd = 100, focusMinutes = 60, focusGoal = 120)
        assertEquals(2, p.classesDone)
        assertEquals(4, p.classesTotal)
        assertEquals(0.5f, p.fraction)
    }

    @Test
    fun dailyProgressCountsDeadlinesDueToday() {
        val tasks = listOf(
            task("done", 50L, done = true),
            task("open", 60L),
            task("tomorrow", 150L),
        )
        val p = dailyProgress(emptyList(), 2, 0, tasks, dayStart = 0, dayEnd = 100, focusMinutes = 0, focusGoal = 120)
        assertEquals(2, p.tasksTotal)
        assertEquals(1, p.tasksDone)
        assertEquals(0.25f, p.fraction) // (0.5 tasks + 0 focus) / 2
    }

    @Test
    fun dailyProgressCapsFocusAndHandlesEmptyDay() {
        val over = dailyProgress(emptyList(), 2, 0, emptyList(), 0, 100, focusMinutes = 500, focusGoal = 120)
        assertEquals(1f, over.fraction)
        val none = dailyProgress(emptyList(), 2, 0, emptyList(), 0, 100, focusMinutes = 0, focusGoal = 0)
        assertEquals(0f, none.fraction)
    }

    @Test
    fun upcomingExamsAreSortedAndFiltered() {
        val tasks = listOf(
            task("late", 300L).copy(isExam = true),
            task("soon", 150L).copy(isExam = true),
            task("past", 50L).copy(isExam = true),
            task("finished", 160L, done = true).copy(isExam = true),
            task("homework", 120L),
            task("removed", 130L, deleted = true).copy(isExam = true),
        )
        assertEquals(listOf("soon", "late"), upcomingExams(tasks, now = 100L, limit = 5).map { it.id })
        assertEquals(listOf("soon"), upcomingExams(tasks, now = 100L, limit = 1).map { it.id })
    }
}
