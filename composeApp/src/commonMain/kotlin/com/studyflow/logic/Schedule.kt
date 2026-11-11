package com.studyflow.logic

import com.studyflow.model.ClassSlot
import com.studyflow.model.StudyTask
import com.studyflow.util.DAY_MS

/** Live (not deleted) classes on a weekday, earliest first. */
fun classesOn(classes: List<ClassSlot>, dayIso: Int): List<ClassSlot> =
    classes.filter { !it.deleted && it.dayOfWeek == dayIso }.sortedBy { it.startMinute }

/** True when two different classes on the same day overlap in time. */
fun overlaps(a: ClassSlot, b: ClassSlot): Boolean =
    a.id != b.id &&
        !a.deleted && !b.deleted &&
        a.dayOfWeek == b.dayOfWeek &&
        a.startMinute < b.endMinute &&
        b.startMinute < a.endMinute

/** The class happening right now, or else the next one today. */
fun nextClass(classes: List<ClassSlot>, dayIso: Int, nowMinute: Int): ClassSlot? =
    classesOn(classes, dayIso).firstOrNull { it.endMinute > nowMinute }

data class TaskGroups(
    val overdue: List<StudyTask>,
    val thisWeek: List<StudyTask>,
    val later: List<StudyTask>,
)

/** Splits not-done tasks into overdue / due within 7 days / later. Each group is sorted by due date. */
fun groupActive(tasks: List<StudyTask>, now: Long): TaskGroups {
    val active = tasks.filter { !it.deleted && !it.done }.sortedBy { it.dueAt }
    return TaskGroups(
        overdue = active.filter { it.dueAt < now },
        thisWeek = active.filter { it.dueAt >= now && it.dueAt < now + 7 * DAY_MS },
        later = active.filter { it.dueAt >= now + 7 * DAY_MS },
    )
}
