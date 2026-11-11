package com.studyflow.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyflow.AppContainer
import com.studyflow.focus.Phase
import com.studyflow.model.AppData
import com.studyflow.util.DAY_SHORT
import com.studyflow.util.formatClock
import com.studyflow.util.toLocal
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

@Composable
fun FocusScreen(container: AppContainer, data: AppData, now: Long) {
    val timerData by container.timer.data.collectAsState()
    val settings by container.settings.state.collectAsState()
    val ui = timerData.ui
    val timer = container.timer

    val isFocus = ui.phase == Phase.FOCUS
    val accent by animateColorAsState(
        targetValue = if (isFocus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        animationSpec = tween(durationMillis = 500),
        label = "phaseColor",
    )
    // breathing glow while the timer runs; the ring itself moves smoothly between ticks
    val pulse = rememberPulse(ui.running)
    val smoothProgress by animateFloatAsState(
        targetValue = ui.progress,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "ringProgress",
    )

    // minutes of focus for each of the last 7 days (oldest first)
    val todayDate = now.toLocal().date
    val week = (6 downTo 0).map { back ->
        val date = todayDate.minus(back, DateTimeUnit.DAY)
        val minutes = data.sessions
            .filter { !it.deleted && it.startedAt.toLocal().date == date }
            .sumOf { it.durationSec } / 60
        Triple(DAY_SHORT[date.dayOfWeek.isoDayNumber - 1], minutes, back == 0)
    }
    val todayMinutes = week.last().second
    val sessionsToday = data.sessions.count { !it.deleted && it.startedAt.toLocal().date == todayDate }
    val bestDay = (week.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    val openTasks = data.tasks.filter { !it.deleted && !it.done }.sortedBy { it.dueAt }.take(4)

    ScreenColumn {
        item { ScreenHeader("Focus", if (isFocus) "One thing at a time" else "Take a proper break") }

        item {
            SoftCard {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        if (isFocus) "FOCUS" else "BREAK",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    Box(
                        Modifier
                            .size(240.dp)
                            .graphicsLayer {
                                val s = 1f + 0.03f * pulse.value
                                scaleX = s
                                scaleY = s
                            }
                            .drawBehind { drawCircle(color = accent.copy(alpha = 0.16f * pulse.value)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = { smoothProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = accent,
                            strokeWidth = 12.dp,
                            trackColor = MaterialTheme.colorScheme.background,
                        )
                        Text(
                            formatClock(ui.remainingSec),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (isFocus) {
                        OutlinedTextField(
                            value = ui.label,
                            onValueChange = { timer.setLabel(it) },
                            label = { Text("What are you working on?") },
                            singleLine = true,
                            enabled = !ui.running,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (ui.isFresh && openTasks.isNotEmpty()) {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                openTasks.forEach { task ->
                                    AssistChip(onClick = { timer.setLabel(task.title) }, label = { Text(task.title) })
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { if (ui.running) timer.pause() else timer.start() }) {
                            Text(if (ui.running) "Pause" else if (ui.isFresh) "Start" else "Resume")
                        }
                        OutlinedButton(onClick = { timer.reset() }, enabled = !ui.isFresh) { Text("Reset") }
                        if (!isFocus) {
                            TextButton(onClick = { timer.skip() }) { Text("Skip break") }
                        }
                    }
                }
            }
        }

        if (ui.isFresh) {
            item {
                SoftCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Focus length", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15, 25, 45, 60).forEach { m ->
                                FilterChip(
                                    selected = settings.focusMinutes == m,
                                    onClick = {
                                        container.settings.update { it.copy(focusMinutes = m) }
                                        timer.setDurations(m, settings.breakMinutes)
                                    },
                                    label = { Text("$m min") },
                                )
                            }
                        }
                        Text("Break length", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 15).forEach { m ->
                                FilterChip(
                                    selected = settings.breakMinutes == m,
                                    onClick = {
                                        container.settings.update { it.copy(breakMinutes = m) }
                                        timer.setDurations(settings.focusMinutes, m)
                                    },
                                    label = { Text("$m min") },
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionLabel("Your focus") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnimatedStatCard(todayMinutes, "minutes today", Modifier.weight(1f))
                AnimatedStatCard(sessionsToday, "sessions today", Modifier.weight(1f))
            }
        }
        item {
            SoftCard {
                Row(
                    Modifier.fillMaxWidth().height(150.dp).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    week.forEach { (label, minutes, isToday) ->
                        Column(
                            Modifier.fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(if (minutes > 0) "$minutes" else "", style = MaterialTheme.typography.labelSmall)
                            Box(
                                Modifier
                                    .width(28.dp)
                                    .height((6f + 70f * minutes / bestDay).dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isToday) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    ),
                            )
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
