package com.studyflow.logic

import com.studyflow.model.ClassSlot
import com.studyflow.model.StudyTask

data class WeekProgress(val done: Int, val total: Int, val overdue: Int) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
}

fun weekProgress(tasks: List<StudyTask>, weekStart: Long, weekEnd: Long, now: Long): WeekProgress {
    val inWeek = tasks.filter { !it.deleted && it.dueAt >= weekStart && it.dueAt < weekEnd }
    return WeekProgress(
        done = inWeek.count { it.done },
        total = inWeek.size,
        overdue = inWeek.count { !it.done && it.dueAt < now },
    )
}

fun focusStreak(daysWithFocus: Set<Long>, today: Long): Int {
    var day = if (today in daysWithFocus) today else today - 1
    var count = 0
    while (day in daysWithFocus) {
        count++
        day--
    }
    return count
}

data class DailyProgress(
    val classesDone: Int,
    val classesTotal: Int,
    val tasksDone: Int,
    val tasksTotal: Int,
    val focusMinutes: Int,
    val focusGoal: Int,
) {
    val fraction: Float
        get() {
            var sum = 0f
            var parts = 0
            if (classesTotal > 0) {
                sum += classesDone.toFloat() / classesTotal
                parts++
            }
            if (tasksTotal > 0) {
                sum += tasksDone.toFloat() / tasksTotal
                parts++
            }
            if (focusGoal > 0) {
                sum += (focusMinutes.toFloat() / focusGoal).coerceAtMost(1f)
                parts++
            }
            return if (parts == 0) 0f else sum / parts
        }
}

fun dailyProgress(
    classes: List<ClassSlot>,
    dayIso: Int,
    nowMinute: Int,
    tasks: List<StudyTask>,
    dayStart: Long,
    dayEnd: Long,
    focusMinutes: Int,
    focusGoal: Int,
): DailyProgress {
    val todays = classesOn(classes, dayIso)
    val dueToday = tasks.filter { !it.deleted && it.dueAt >= dayStart && it.dueAt < dayEnd }
    return DailyProgress(
        classesDone = todays.count { it.endMinute <= nowMinute },
        classesTotal = todays.size,
        tasksDone = dueToday.count { it.done },
        tasksTotal = dueToday.size,
        focusMinutes = focusMinutes,
        focusGoal = focusGoal,
    )
}

fun upcomingExams(tasks: List<StudyTask>, now: Long, limit: Int): List<StudyTask> =
    tasks
        .filter { !it.deleted && it.isExam && !it.done && it.dueAt >= now }
        .sortedBy { it.dueAt }
        .take(limit)
