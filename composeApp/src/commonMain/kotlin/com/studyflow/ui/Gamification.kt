package com.studyflow.logic

import com.studyflow.model.AppData
import com.studyflow.model.Priority
import com.studyflow.model.StudyTask
import com.studyflow.model.XpEvent

/** What kind of reward an [XpEvent] is. */
object XpKind {
    const val TASK = "task"
    const val EXAM = "exam"
    const val FOCUS = "focus"
    const val DAILY = "daily"
    const val BADGE = "badge"
    const val CHALLENGE = "challenge"
}

// ---------------------------------------------------------------- levels

/** XP needed to reach a level: 0, 100, 300, 600, 1000, 1500 ... (level 5 starts at 1000 XP). */
fun xpForLevel(level: Int): Int = 50 * level * (level - 1)

fun levelForXp(xp: Int): Int {
    var level = 1
    while (xpForLevel(level + 1) <= xp) level++
    return level
}

private val LEVEL_TITLES = listOf("Fresh Start", "Learner", "Scholar", "Achiever", "Strategist", "Expert", "Master", "Legend")

fun levelTitle(level: Int): String = LEVEL_TITLES[(level - 1).coerceIn(0, LEVEL_TITLES.lastIndex)]

data class LevelInfo(
    val level: Int,
    val title: String,
    val xp: Int,
    val startXp: Int,
    val nextXp: Int,
) {
    /** 0f..1f progress between this level and the next. */
    val fraction: Float
        get() = if (nextXp <= startXp) 1f else ((xp - startXp).toFloat() / (nextXp - startXp)).coerceIn(0f, 1f)

    val xpToNext: Int get() = (nextXp - xp).coerceAtLeast(0)
}

fun levelInfo(xp: Int): LevelInfo {
    val safe = xp.coerceAtLeast(0)
    val level = levelForXp(safe)
    return LevelInfo(level, levelTitle(level), safe, xpForLevel(level), xpForLevel(level + 1))
}

fun totalXp(events: List<XpEvent>): Int = events.filter { !it.deleted }.sumOf { it.amount }
fun totalXp(data: AppData): Int = totalXp(data.xp)

// ---------------------------------------------------------------- XP amounts

const val DAILY_BONUS_XP = 10
const val BADGE_BONUS_XP = 25

fun taskXp(task: StudyTask): Int =
    if (task.isExam) {
        40
    } else {
        when (task.priority) {
            Priority.LOW -> 10
            Priority.MEDIUM -> 15
            Priority.HIGH -> 20
        }
    }

/** One XP per focused minute, at least 5 and at most 60 per session. */
fun focusXp(minutes: Int): Int = minutes.coerceIn(5, 60)

// ---------------------------------------------------------------- badges

data class GameStats(
    val tasksDone: Int,
    val examsDone: Int,
    val focusSessions: Int,
    val focusMinutes: Int,
    val streakDays: Int,
    val classes: Int,
)

class BadgeDef(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val progress: (GameStats) -> Int,
)

val BADGES: List<BadgeDef> = listOf(
    BadgeDef("first_step", "First Step", "Finish your first deadline", 1) { it.tasksDone },
    BadgeDef("focus_starter", "Focus Starter", "Complete your first focus session", 1) { it.focusSessions },
    BadgeDef("focus_master", "Focus Master", "Complete 3 focus sessions", 3) { it.focusSessions },
    BadgeDef("on_a_roll", "On a Roll", "Study 3 days in a row", 3) { it.streakDays },
    BadgeDef("week_warrior", "Week Warrior", "Keep a 7-day study streak", 7) { it.streakDays },
    BadgeDef("task_crusher", "Task Crusher", "Finish 10 deadlines", 10) { it.tasksDone },
    BadgeDef("marathon", "Marathon", "Focus for 5 hours in total", 300) { it.focusMinutes },
    BadgeDef("exam_ace", "Exam Ace", "Finish 3 exams", 3) { it.examsDone },
    BadgeDef("planner", "Planner", "Add 5 classes to your timetable", 5) { it.classes },
)

fun badgeById(id: String): BadgeDef? = BADGES.firstOrNull { it.id == id }

// ---------------------------------------------------------------- weekly challenges

class ChallengeDef(
    val kind: String,
    val title: String,
    val description: String,
    val target: Int,
    val xp: Int,
)

private val CHALLENGE_POOL = listOf(
    ChallengeDef("sessions", "Focus Fiesta", "Complete 5 focus sessions", 5, 60),
    ChallengeDef("tasks", "Deadline Slayer", "Finish 4 deadlines", 4, 50),
    ChallengeDef("days", "Show Up", "Study on 4 different days", 4, 60),
    ChallengeDef("minutes", "Deep Dive", "Focus for 120 minutes", 120, 70),
)

/** Three challenges per week, rotating through the pool so that weeks feel different. */
fun challengesForWeek(weekIndex: Long): List<ChallengeDef> {
    val start = weekIndex.mod(CHALLENGE_POOL.size.toLong()).toInt()
    return List(3) { CHALLENGE_POOL[(start + it) % CHALLENGE_POOL.size] }
}

data class WeekStats(val sessions: Int, val tasks: Int, val days: Int, val minutes: Int)

fun ChallengeDef.progressIn(week: WeekStats): Int = when (kind) {
    "sessions" -> week.sessions
    "tasks" -> week.tasks
    "days" -> week.days
    else -> week.minutes
}

// ---------------------------------------------------------------- evaluating

private fun isTaskKind(kind: String) = kind == XpKind.TASK || kind == XpKind.EXAM

private fun statsFor(events: List<XpEvent>, data: AppData, now: Long, dayOf: (Long) -> Long): GameStats {
    val live = events.filter { !it.deleted }
    val focus = live.filter { it.kind == XpKind.FOCUS }
    val focusDays = data.sessions.filter { !it.deleted }.map { dayOf(it.startedAt) }.toSet()
    return GameStats(
        tasksDone = live.count { isTaskKind(it.kind) },
        examsDone = live.count { it.kind == XpKind.EXAM },
        focusSessions = focus.size,
        focusMinutes = focus.sumOf { it.meta },
        streakDays = focusStreak(focusDays, dayOf(now)),
        classes = data.classes.count { !it.deleted },
    )
}

private fun weekStatsFor(events: List<XpEvent>, data: AppData, weekStart: Long, weekEnd: Long, dayOf: (Long) -> Long): WeekStats {
    val inWeek = events.filter { !it.deleted && it.at >= weekStart && it.at < weekEnd }
    val focus = inWeek.filter { it.kind == XpKind.FOCUS }
    val days = data.sessions
        .filter { !it.deleted && it.startedAt >= weekStart && it.startedAt < weekEnd }
        .map { dayOf(it.startedAt) }
        .toSet()
    return WeekStats(
        sessions = focus.size,
        tasks = inWeek.count { isTaskKind(it.kind) },
        days = days.size,
        minutes = focus.sumOf { it.meta },
    )
}

/**
 * Works out which rewards the student has earned but not yet been given. The ids are fixed
 * ("task:<id>", "badge:focus_master" ...), so calling this again right after returns nothing,
 * and two devices that both award the same thing produce the same event.
 *
 * @param dayOf turns a time into a day number (the app passes the device-time-zone day).
 */
fun missingEvents(data: AppData, now: Long, weekStart: Long, weekEnd: Long, dayOf: (Long) -> Long): List<XpEvent> {
    val have = data.xp.filter { !it.deleted }.map { it.id }.toMutableSet()
    val created = ArrayList<XpEvent>()
    fun award(event: XpEvent) {
        if (have.add(event.id)) created += event
    }

    // XP for finished deadlines and exams
    for (task in data.tasks) {
        if (task.deleted || !task.done) continue
        val at = minOf(task.updatedAt, now)
        award(
            XpEvent(
                id = "task:${task.id}",
                kind = if (task.isExam) XpKind.EXAM else XpKind.TASK,
                label = (if (task.isExam) "Exam finished: " else "Finished: ") + task.title,
                amount = taskXp(task),
                at = at,
                updatedAt = at,
            ),
        )
    }

    // XP for focus sessions, plus a daily bonus for the first session of each day
    val sessions = data.sessions.filter { !it.deleted }
    for (session in sessions) {
        val minutes = session.durationSec / 60
        award(
            XpEvent(
                id = "focus:${session.id}",
                kind = XpKind.FOCUS,
                label = "Focus session: $minutes min",
                amount = focusXp(minutes),
                at = session.startedAt,
                meta = minutes,
                updatedAt = session.startedAt,
            ),
        )
    }
    for ((day, list) in sessions.groupBy { dayOf(it.startedAt) }) {
        val first = list.minOf { it.startedAt }
        award(XpEvent("daily:$day", XpKind.DAILY, "Daily study bonus", DAILY_BONUS_XP, at = first, updatedAt = first))
    }

    // badges
    val withNew = data.xp + created
    val stats = statsFor(withNew, data, now, dayOf)
    for (badge in BADGES) {
        if (badge.progress(stats) >= badge.target) {
            award(
                XpEvent(
                    id = "badge:${badge.id}",
                    kind = XpKind.BADGE,
                    label = "Badge unlocked: ${badge.title}",
                    amount = BADGE_BONUS_XP,
                    at = now,
                    updatedAt = now,
                ),
            )
        }
    }

    // this week's challenges
    val weekDay = dayOf(weekStart)
    val week = weekStatsFor(withNew, data, weekStart, weekEnd, dayOf)
    for (challenge in challengesForWeek(weekDay.floorDiv(7L))) {
        if (challenge.progressIn(week) >= challenge.target) {
            award(
                XpEvent(
                    id = "challenge:$weekDay:${challenge.kind}",
                    kind = XpKind.CHALLENGE,
                    label = "Challenge complete: ${challenge.title}",
                    amount = challenge.xp,
                    at = now,
                    updatedAt = now,
                ),
            )
        }
    }
    return created
}

// ---------------------------------------------------------------- what the screens show

data class BadgeState(val def: BadgeDef, val unlocked: Boolean, val progress: Int)
data class ChallengeState(val def: ChallengeDef, val progress: Int, val done: Boolean)

data class GameSnapshot(
    val level: LevelInfo,
    val streak: Int,
    val badges: List<BadgeState>,
    val challenges: List<ChallengeState>,
    val recent: List<XpEvent>,
)

fun gameSnapshot(data: AppData, now: Long, weekStart: Long, weekEnd: Long, dayOf: (Long) -> Long): GameSnapshot {
    val events = data.xp.filter { !it.deleted }
    val ids = events.map { it.id }.toSet()
    val stats = statsFor(events, data, now, dayOf)
    val week = weekStatsFor(events, data, weekStart, weekEnd, dayOf)
    val weekDay = dayOf(weekStart)

    return GameSnapshot(
        level = levelInfo(totalXp(data)),
        streak = stats.streakDays,
        badges = BADGES.map { badge ->
            BadgeState(badge, unlocked = "badge:${badge.id}" in ids, progress = badge.progress(stats))
        },
        challenges = challengesForWeek(weekDay.floorDiv(7L)).map { challenge ->
            val progress = challenge.progressIn(week)
            ChallengeState(challenge, progress, done = "challenge:$weekDay:${challenge.kind}" in ids)
        },
        recent = events.sortedByDescending { it.at }.take(6),
    )
}

// ---------------------------------------------------------------- pop-ups

enum class CelebrationKind { XP, BADGE, CHALLENGE, LEVEL }

data class Celebration(
    val id: Long,
    val kind: CelebrationKind,
    val title: String,
    val subtitle: String,
    /** Set for badge pop-ups so the matching badge icon can be shown. */
    val badgeId: String? = null,
)

/** Turns freshly awarded events into the messages the student sees. */
fun celebrationsFor(created: List<XpEvent>, xpBefore: Int, xpAfter: Int, firstId: Long): List<Celebration> {
    val out = ArrayList<Celebration>()
    var id = firstId

    val xpGained = created
        .filter { it.kind == XpKind.TASK || it.kind == XpKind.EXAM || it.kind == XpKind.FOCUS || it.kind == XpKind.DAILY }
    if (xpGained.isNotEmpty()) {
        val main = xpGained.firstOrNull { it.kind != XpKind.DAILY } ?: xpGained.first()
        out += Celebration(id++, CelebrationKind.XP, "+${xpGained.sumOf { it.amount }} XP", main.label)
    }
    for (event in created.filter { it.kind == XpKind.BADGE }) {
        val badge = badgeById(event.id.removePrefix("badge:"))
        out += Celebration(id++, CelebrationKind.BADGE, "Badge unlocked: ${badge?.title ?: "New badge"}", badge?.description ?: "", badge?.id)
    }
    for (event in created.filter { it.kind == XpKind.CHALLENGE }) {
        out += Celebration(id++, CelebrationKind.CHALLENGE, "Weekly challenge complete", "${event.label.removePrefix("Challenge complete: ")}  ·  +${event.amount} XP")
    }
    val before = levelForXp(xpBefore)
    val after = levelForXp(xpAfter)
    if (after > before) {
        out += Celebration(id++, CelebrationKind.LEVEL, "Level $after reached", "You are now a ${levelTitle(after)}")
    }
    return out
}
