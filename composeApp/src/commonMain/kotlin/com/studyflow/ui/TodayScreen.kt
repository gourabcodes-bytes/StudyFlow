package com.studyflow.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.studyflow.logic.classesOn
import com.studyflow.logic.dailyProgress
import com.studyflow.logic.dayBounds
import com.studyflow.logic.epochDay
import com.studyflow.logic.focusStreak
import com.studyflow.logic.groupActive
import com.studyflow.logic.nextClass
import com.studyflow.logic.upcomingExams
import com.studyflow.logic.weekBounds
import com.studyflow.logic.weekProgress
import com.studyflow.model.AppData
import com.studyflow.model.StudyTask
import com.studyflow.util.formatDateLong
import com.studyflow.util.isoDay
import com.studyflow.util.minuteOfDay
import com.studyflow.util.toLocal

private const val DAILY_FOCUS_GOAL_MIN = 120

@Composable
fun TodayScreen(
    data: AppData,
    now: Long,
    userName: String,
    onOpenTab: (Tab) -> Unit,
    onToggleTask: (StudyTask, Boolean) -> Unit,
    onEditTask: (StudyTask) -> Unit,
) {
    val local = now.toLocal()
    val dayIso = isoDay(local)
    val nowMinute = minuteOfDay(local)

    val todays = classesOn(data.classes, dayIso)
    val upcoming = nextClass(data.classes, dayIso, nowMinute)

    val (weekStart, weekEnd) = weekBounds(now)
    val week = weekProgress(data.tasks, weekStart, weekEnd, now)
    val groups = groupActive(data.tasks, now)
    val soon = groups.overdue + groups.thisWeek
    val exams = upcomingExams(data.tasks, now, limit = 5)

    val sessions = data.sessions.filter { !it.deleted }
    val focusToday = sessions.filter { it.startedAt.toLocal().date == local.date }.sumOf { it.durationSec } / 60
    val streak = focusStreak(sessions.map { epochDay(it.startedAt) }.toSet(), epochDay(now))

    val (dayStart, dayEnd) = dayBounds(now)
    val daily = dailyProgress(
        classes = data.classes,
        dayIso = dayIso,
        nowMinute = nowMinute,
        tasks = data.tasks,
        dayStart = dayStart,
        dayEnd = dayEnd,
        focusMinutes = focusToday,
        focusGoal = DAILY_FOCUS_GOAL_MIN,
    )

    ScreenColumn {
        item {
            val greeting = if (userName.isBlank()) "Welcome back" else "Hello, $userName"
            DashboardHeader(greeting = greeting, dateText = formatDateLong(local), streak = streak)
        }

        item { NextClassHero(upcoming = upcoming, nowMinute = nowMinute, hasClassesToday = todays.isNotEmpty()) }

        item { DailyProgressCard(progress = daily) }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RingStatCard(
                    title = "THIS WEEK'S DEADLINES",
                    value = if (week.total == 0) "100%" else "${(week.fraction * 100).toInt()}%",
                    caption = if (week.total == 0) "No deadlines this week" else "${week.done} of ${week.total} finished",
                    fraction = if (week.total == 0) 1f else week.fraction,
                    ringColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                RingStatCard(
                    title = "TODAY'S FOCUS TIME",
                    value = if (focusToday >= 60) "${focusToday / 60}h ${focusToday % 60}m" else "${focusToday}m",
                    caption = if (focusToday >= DAILY_FOCUS_GOAL_MIN) "Daily goal hit!" else "${DAILY_FOCUS_GOAL_MIN - focusToday}m left to goal",
                    fraction = (focusToday.toFloat() / DAILY_FOCUS_GOAL_MIN).coerceIn(0f, 1f),
                    ringColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (exams.isNotEmpty()) {
            item {
                SectionLabel("Exams coming up")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    exams.forEach { exam ->
                        val daysLeft = (epochDay(exam.dueAt) - epochDay(now)).coerceAtLeast(0L)
                        ExamCard(
                            exam = exam,
                            daysLeft = daysLeft,
                            onClick = {
                                onOpenTab(Tab.DEADLINES)
                                onEditTask(exam)
                            },
                        )
                    }
                }
            }
        }

        item { SectionLabel("Schedule for today") }
        if (todays.isEmpty()) {
            item { EmptyHint("No classes today. Enjoy your day!") }
        } else {
            itemsIndexed(todays, key = { _, item -> item.id }) { _, slot ->
                val inClass = slot.startMinute <= nowMinute && nowMinute < slot.endMinute
                ClassCard(slot = slot, highlight = inClass)
            }
        }

        item { SectionLabel("Deadlines coming up") }
        if (soon.isEmpty()) {
            item { EmptyHint("No deadlines due in the next few days.") }
        } else {
            itemsIndexed(soon.take(5), key = { _, task -> task.id }) { _, task ->
                TaskRow(
                    task = task,
                    now = now,
                    onToggle = { done -> onToggleTask(task, done) },
                    onClick = {
                        onOpenTab(Tab.DEADLINES)
                        onEditTask(task)
                    },
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onOpenTab(Tab.FOCUS) }, modifier = Modifier.weight(1f)) {
                    Text("Start Focus Timer")
                }
                OutlinedButton(onClick = { onOpenTab(Tab.ASSISTANT) }, modifier = Modifier.weight(1f)) {
                    Text("Ask Study Assistant")
                }
            }
        }
    }
}
