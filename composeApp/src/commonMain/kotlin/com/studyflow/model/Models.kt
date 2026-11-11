package com.studyflow.model

import kotlinx.serialization.Serializable

/**
 * Everything that is synced between devices implements [Syncable].
 * Deleting an item never removes it: it is marked [deleted] (a "tombstone") so the
 * deletion can travel to the other device during sync.
 */
interface Syncable {
    val id: String
    val updatedAt: Long
    val deleted: Boolean
}

/** One weekly class in the timetable. dayOfWeek: 1 = Monday ... 7 = Sunday. */
@Serializable
data class ClassSlot(
    override val id: String,
    val title: String,
    val location: String = "",
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val colorIndex: Int = 0,
    override val updatedAt: Long = 0L,
    override val deleted: Boolean = false,
) : Syncable

enum class Priority { LOW, MEDIUM, HIGH }

/** [StudyTask.notifyMinutes]: follow the default in Settings. */
const val NOTIFY_DEFAULT = -1

/** [StudyTask.notifyMinutes]: no reminder for this deadline. */
const val NOTIFY_OFF = -2

/** A deadline / assignment. */
@Serializable
data class StudyTask(
    override val id: String,
    val title: String,
    val course: String = "",
    val notes: String = "",
    val dueAt: Long,
    val priority: Priority = Priority.MEDIUM,
    /** Exams get their own countdown cards on the dashboard. */
    val isExam: Boolean = false,
    /** Minutes before the due time to remind, or [NOTIFY_DEFAULT] / [NOTIFY_OFF]. */
    val notifyMinutes: Int = NOTIFY_DEFAULT,
    val done: Boolean = false,
    override val updatedAt: Long = 0L,
    override val deleted: Boolean = false,
) : Syncable

/** A completed focus session. */
@Serializable
data class FocusSession(
    override val id: String,
    val label: String = "",
    val startedAt: Long,
    val durationSec: Int,
    override val updatedAt: Long = 0L,
    override val deleted: Boolean = false,
) : Syncable

/** A saved study note (for example an assistant answer worth keeping). Works fully offline. */
@Serializable
data class Note(
    override val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    override val updatedAt: Long = 0L,
    override val deleted: Boolean = false,
) : Syncable

/**
 * One reward: XP earned, a badge unlocked or a challenge completed. The id is built from what earned
 * it (for example "task:<id>"), so the same achievement can never be awarded twice, even when two
 * devices award it independently and then sync.
 */
@Serializable
data class XpEvent(
    override val id: String,
    /** "task", "exam", "focus", "daily", "badge" or "challenge" */
    val kind: String,
    val label: String,
    val amount: Int,
    val at: Long = 0L,
    /** Minutes for focus sessions. */
    val meta: Int = 0,
    override val updatedAt: Long = 0L,
    override val deleted: Boolean = false,
) : Syncable

/** The whole synced database. */
@Serializable
data class AppData(
    val classes: List<ClassSlot> = emptyList(),
    val tasks: List<StudyTask> = emptyList(),
    val sessions: List<FocusSession> = emptyList(),
    val xp: List<XpEvent> = emptyList(),
    val notes: List<Note> = emptyList(),
)

/** Device-local settings. These are NOT synced. */
@Serializable
data class Settings(
    val syncHost: String = "",
    val syncCode: String = "",
    val hostCode: String = "",
    val autoSync: Boolean = false,
    val lastSyncAt: Long = 0L,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    /** 0 = follow the phone, 1 = light, 2 = dark */
    val themeMode: Int = 0,
    val accentIndex: Int = 0,
    /** The accent colour as an ARGB number. Default: indigo. */
    val accentSeed: Int = 0xFF4F46E5.toInt(),
    val notificationsOn: Boolean = true,
    /** Minutes before a deadline to remind, unless a deadline says otherwise. */
    val defaultRemindMinutes: Int = 60,
    val lastBackupAt: Long = 0L,
    /** Shown in the greeting on the Today screen. */
    val userName: String = "",
    /** Anthropic API key for the study assistant. Stored on this device only, never synced. */
    val apiKey: String = "",
    val aiModel: String = "claude-3-5-sonnet-20241022",
    /** Optional, e.g. "1st year ECE". Helps the assistant pitch its explanations. */
    val studyProfile: String = "",
)
