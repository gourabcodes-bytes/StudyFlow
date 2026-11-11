package com.studyflow.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studyflow.logic.DailyProgress
import com.studyflow.model.ClassSlot
import com.studyflow.model.StudyTask
import com.studyflow.util.MINUTE_MS
import com.studyflow.util.formatDue
import com.studyflow.util.formatMinute
import com.studyflow.util.humanDuration

/** Greeting on the left, study streak on the right. */
@Composable
fun DashboardHeader(greeting: String, dateText: String, streak: Int) {
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(dateText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StreakPill(streak)
    }
}

@Composable
fun StreakPill(days: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Flame(Modifier.size(18.dp), lit = days > 0)
        Text(
            text = when (days) {
                0 -> "No streak yet"
                1 -> "1 day"
                else -> "$days days"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** A small flame drawn with two curves: orange when the streak is alive, grey otherwise. */
@Composable
private fun Flame(modifier: Modifier, lit: Boolean) {
    val outer = if (lit) Color(0xFFF97316) else MaterialTheme.colorScheme.outline
    val inner = if (lit) Color(0xFFFDE047) else MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val body = Path().apply {
            moveTo(w * 0.50f, h * 0.02f)
            cubicTo(w * 0.95f, h * 0.40f, w * 0.90f, h * 0.98f, w * 0.50f, h * 0.98f)
            cubicTo(w * 0.10f, h * 0.98f, w * 0.05f, h * 0.45f, w * 0.50f, h * 0.02f)
            close()
        }
        drawPath(body, outer)
        val core = Path().apply {
            moveTo(w * 0.50f, h * 0.45f)
            cubicTo(w * 0.75f, h * 0.65f, w * 0.72f, h * 0.98f, w * 0.50f, h * 0.98f)
            cubicTo(w * 0.28f, h * 0.98f, w * 0.25f, h * 0.68f, w * 0.50f, h * 0.45f)
            close()
        }
        drawPath(core, inner)
    }
}

/** The big gradient card: the class you are in now, or the next one, with a countdown. */
@Composable
fun NextClassHero(
    upcoming: ClassSlot?,
    nowMinute: Int,
    hasClassesToday: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val brush = Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
    val textColor = scheme.onPrimary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.background(brush).fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (upcoming == null) {
                Text("ALL CLEAR", style = MaterialTheme.typography.labelLarge, color = textColor.copy(alpha = 0.8f))
                Text(
                    if (hasClassesToday) "Classes are done for today" else "No classes today",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
                Text(
                    if (hasClassesToday) "Nice work. Use the evening to get ahead on a deadline."
                    else "A free day: a good moment for a focus session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.9f),
                )
            } else {
                val inClass = upcoming.startMinute <= nowMinute
                Text(
                    if (inClass) "IN CLASS NOW" else "UP NEXT",
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor.copy(alpha = 0.8f),
                )
                Text(
                    upcoming.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val where = if (upcoming.location.isBlank()) "" else "  ·  ${upcoming.location}"
                Text(
                    "${formatMinute(upcoming.startMinute)} – ${formatMinute(upcoming.endMinute)}$where",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(6.dp))
                val minutes = if (inClass) upcoming.endMinute - nowMinute else upcoming.startMinute - nowMinute
                Text(
                    (if (inClass) "Ends in " else "Starts in ") + humanDuration(minutes * MINUTE_MS),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
                if (inClass) {
                    val total = (upcoming.endMinute - upcoming.startMinute).coerceAtLeast(1)
                    LinearProgressIndicator(
                        progress = { ((nowMinute - upcoming.startMinute).toFloat() / total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = textColor,
                        trackColor = textColor.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

/** A circular progress ring that animates from empty to [fraction] when it appears or changes. */
@Composable
fun ProgressRing(
    fraction: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    content: @Composable () -> Unit,
) {
    var goal by remember { mutableStateOf(0f) }
    LaunchedEffect(fraction) { goal = fraction.coerceIn(0f, 1f) }
    val animated by animateFloatAsState(
        targetValue = goal,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "ring",
    )

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcTopLeft = Offset(inset, inset)
            val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = stroke,
            )
            if (animated > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        content()
    }
}

/** A card with a title, a progress ring with a value in the middle, and a one-line caption. */
@Composable
fun RingStatCard(
    title: String,
    value: String,
    caption: String,
    fraction: Float,
    ringColor: Color,
    modifier: Modifier = Modifier,
) {
    SoftCard(modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            ProgressRing(
                fraction = fraction,
                color = ringColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(112.dp),
            ) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The "how is my day going" card: one percentage built from classes, deadlines and focus time. */
@Composable
fun DailyProgressCard(progress: DailyProgress, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val brush = Brush.linearGradient(listOf(scheme.secondary, scheme.primary))
    val textColor = scheme.onPrimary

    // the percentage counts up from 0 when the card appears or the value changes
    val percent = (progress.fraction * 100).toInt()
    var goal by remember { mutableStateOf(0) }
    LaunchedEffect(percent) { goal = percent }
    val shownPercent by animateIntAsState(
        targetValue = goal,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "dailyPercent",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier.background(brush).fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ProgressRing(
                fraction = progress.fraction,
                color = textColor,
                trackColor = textColor.copy(alpha = 0.25f),
                modifier = Modifier.size(104.dp),
                strokeWidth = 11.dp,
            ) {
                Text(
                    "$shownPercent%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("TODAY'S PROGRESS", style = MaterialTheme.typography.labelLarge, color = textColor.copy(alpha = 0.8f))
                if (progress.classesTotal > 0) {
                    Text(
                        "${progress.classesDone} of ${progress.classesTotal} classes done",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
                }
                if (progress.tasksTotal > 0) {
                    Text(
                        "${progress.tasksDone} of ${progress.tasksTotal} deadlines finished",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
                }
                Text(
                    "${progress.focusMinutes} of ${progress.focusGoal} focus minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        }
    }
}

/** A countdown card for one exam. The colour gets warmer the closer the exam is. */
@Composable
fun ExamCard(exam: StudyTask, daysLeft: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = when {
        daysLeft <= 2 -> listOf(Color(0xFFEF4444), Color(0xFFF97316))
        daysLeft <= 7 -> listOf(Color(0xFFD97706), Color(0xFFEAB308))
        else -> listOf(Color(0xFF4F46E5), Color(0xFF0D9488))
    }
    val countdown = when (daysLeft) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> "$daysLeft days"
    }

    Card(
        modifier = modifier.width(220.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.background(Brush.linearGradient(colors)).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("EXAM", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
            Text(
                exam.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (exam.course.isNotBlank()) {
                Text(exam.course, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
            }
            Spacer(Modifier.height(6.dp))
            Text(countdown, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(formatDue(exam.dueAt), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
        }
    }
}
