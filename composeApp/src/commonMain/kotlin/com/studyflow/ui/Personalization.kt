package com.studyflow.logic

import com.studyflow.model.NOTIFY_DEFAULT
import com.studyflow.model.NOTIFY_OFF
import com.studyflow.model.StudyTask
import com.studyflow.util.MINUTE_MS
import com.studyflow.util.humanDuration

// ---------------------------------------------------------------- reminders

/** One notification to show at time [at]. */
data class Reminder(val id: Int, val at: Long, val title: String, val text: String)

/** The choices for "remind me before a deadline": minutes to label. */
val RemindChoices: List<Pair<Int, String>> = listOf(
    0 to "At due time",
    60 to "1 hour before",
    180 to "3 hours before",
    1440 to "1 day before",
)

/** Everything one deadline needs to be reminded, or null if it should stay quiet. */
fun reminderFor(task: StudyTask, enabled: Boolean, defaultMinutes: Int, now: Long): Reminder? {
    if (!enabled || task.deleted || task.done) return null
    val minutes = when (task.notifyMinutes) {
        NOTIFY_OFF -> return null
        NOTIFY_DEFAULT -> defaultMinutes
        else -> task.notifyMinutes
    }
    val at = task.dueAt - minutes * MINUTE_MS
    if (at <= now) return null
    val what = if (task.isExam) "Exam coming up" else "Deadline coming up"
    val when_ = if (minutes <= 0) "due now" else "due in ${humanDuration(minutes * MINUTE_MS)}"
    return Reminder(id = task.id.hashCode(), at = at, title = what, text = "${task.title} · $when_")
}

/** The reminders to schedule, soonest first. At most [limit], so the phone is never flooded. */
fun plannedReminders(
    tasks: List<StudyTask>,
    enabled: Boolean,
    defaultMinutes: Int,
    now: Long,
    limit: Int = 50,
): List<Reminder> =
    tasks.mapNotNull { reminderFor(it, enabled, defaultMinutes, now) }
        .sortedBy { it.at }
        .take(limit)

// ---------------------------------------------------------------- notes

private val MARKDOWN_LINE_START = Regex("^\\s*(#{1,6}\\s+|[-*\u2022]\\s+|\\d+[.)]\\s+)")

private fun stripMarkdown(line: String): String =
    line.replace(MARKDOWN_LINE_START, "").replace("**", "").replace("`", "").trim()

/** A short plain-text preview of a note for lists. */
fun notePreview(body: String, maxChars: Int = 140): String {
    val flat = body.lines().map { stripMarkdown(it) }.filter { it.isNotEmpty() }.joinToString(" ")
    return if (flat.length <= maxChars) flat else flat.take(maxChars).trimEnd() + "…"
}

/** A title for a note made from an assistant answer: its first meaningful line. */
fun noteTitleFrom(text: String, maxChars: Int = 60): String {
    val first = text.lines().map { stripMarkdown(it) }.firstOrNull { it.isNotEmpty() } ?: return "Untitled note"
    return if (first.length <= maxChars) first else first.take(maxChars).trimEnd() + "…"
}
