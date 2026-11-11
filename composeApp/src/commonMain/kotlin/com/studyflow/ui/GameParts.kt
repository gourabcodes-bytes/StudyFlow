package com.studyflow.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyflow.logic.LevelInfo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ------------------------------------------------------------------ colours

val XpBlue = Color(0xFF3B82F6)
val XpTeal = Color(0xFF14B8A6)
val XpGreen = Color(0xFF22C55E)
val GoldLight = Color(0xFFFFD54F)
val GoldDark = Color(0xFFFFA000)

private fun badgeColor(id: String): Color = when (id) {
    "first_step" -> Color(0xFF22C55E)
    "focus_starter" -> Color(0xFF3B82F6)
    "focus_master" -> Color(0xFFEF4444)
    "on_a_roll" -> Color(0xFFF97316)
    "week_warrior" -> Color(0xFFCA8A04)
    "task_crusher" -> Color(0xFF8B5CF6)
    "marathon" -> Color(0xFF0EA5E9)
    "exam_ace" -> Color(0xFFEC4899)
    "planner" -> Color(0xFF14B8A6)
    else -> Color(0xFF6366F1)
}

// ------------------------------------------------------------------ badge icons

private fun pentagonPath(center: Offset, radius: Float): Path = Path().apply {
    for (i in 0 until 5) {
        val angle = (-90.0 + 72.0 * i) * PI / 180.0
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun starPath(center: Offset, outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = (-90.0 + 36.0 * i) * PI / 180.0
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.drawBadgeGlyph(id: String, c: Offset, r: Float) {
    val white = Color.White
    val line = Stroke(width = r * 0.17f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (id) {
        "first_step" -> {
            val tick = Path().apply {
                moveTo(c.x - r * 0.50f, c.y + r * 0.02f)
                lineTo(c.x - r * 0.12f, c.y + r * 0.40f)
                lineTo(c.x + r * 0.55f, c.y - r * 0.38f)
            }
            drawPath(tick, white, style = line)
        }

        "focus_starter" -> {
            drawCircle(white, radius = r * 0.52f, center = c, style = line)
            drawLine(white, c, Offset(c.x, c.y - r * 0.32f), strokeWidth = r * 0.17f, cap = StrokeCap.Round)
            drawLine(white, c, Offset(c.x + r * 0.24f, c.y + r * 0.10f), strokeWidth = r * 0.17f, cap = StrokeCap.Round)
        }

        "focus_master" -> {
            drawCircle(white, radius = r * 0.58f, center = c, style = line)
            drawCircle(white, radius = r * 0.30f, center = c, style = line)
            drawCircle(white, radius = r * 0.09f, center = c)
        }

        "on_a_roll" -> {
            val flame = Path().apply {
                moveTo(c.x, c.y - r * 0.62f)
                cubicTo(c.x + r * 0.62f, c.y - r * 0.05f, c.x + r * 0.50f, c.y + r * 0.62f, c.x, c.y + r * 0.62f)
                cubicTo(c.x - r * 0.50f, c.y + r * 0.62f, c.x - r * 0.62f, c.y - r * 0.05f, c.x, c.y - r * 0.62f)
                close()
            }
            drawPath(flame, white)
        }

        "week_warrior" -> drawPath(starPath(c, r * 0.66f, r * 0.28f), white)

        "task_crusher" -> {
            val bolt = Path().apply {
                moveTo(c.x + r * 0.10f, c.y - r * 0.66f)
                lineTo(c.x - r * 0.36f, c.y + r * 0.08f)
                lineTo(c.x - r * 0.02f, c.y + r * 0.08f)
                lineTo(c.x - r * 0.12f, c.y + r * 0.66f)
                lineTo(c.x + r * 0.36f, c.y - r * 0.10f)
                lineTo(c.x + r * 0.02f, c.y - r * 0.10f)
                close()
            }
            drawPath(bolt, white)
        }

        "marathon" -> {
            val top = Path().apply {
                moveTo(c.x - r * 0.42f, c.y - r * 0.58f)
                lineTo(c.x + r * 0.42f, c.y - r * 0.58f)
                lineTo(c.x, c.y)
                close()
            }
            val bottom = Path().apply {
                moveTo(c.x - r * 0.42f, c.y + r * 0.58f)
                lineTo(c.x + r * 0.42f, c.y + r * 0.58f)
                lineTo(c.x, c.y)
                close()
            }
            drawPath(top, white)
            drawPath(bottom, white)
        }

        "exam_ace" -> {
            val cap = Path().apply {
                moveTo(c.x, c.y - r * 0.46f)
                lineTo(c.x + r * 0.72f, c.y - r * 0.10f)
                lineTo(c.x, c.y + r * 0.22f)
                lineTo(c.x - r * 0.72f, c.y - r * 0.10f)
                close()
            }
            val base = Path().apply {
                moveTo(c.x - r * 0.38f, c.y + r * 0.08f)
                lineTo(c.x - r * 0.38f, c.y + r * 0.38f)
                quadraticTo(c.x, c.y + r * 0.66f, c.x + r * 0.38f, c.y + r * 0.38f)
                lineTo(c.x + r * 0.38f, c.y + r * 0.08f)
                close()
            }
            drawPath(cap, white)
            drawPath(base, white.copy(alpha = 0.85f))
        }

        "planner" -> {
            drawRoundRect(
                color = white,
                topLeft = Offset(c.x - r * 0.52f, c.y - r * 0.44f),
                size = Size(r * 1.04f, r * 0.96f),
                cornerRadius = CornerRadius(r * 0.16f),
                style = line,
            )
            drawLine(white, Offset(c.x - r * 0.52f, c.y - r * 0.10f), Offset(c.x + r * 0.52f, c.y - r * 0.10f), strokeWidth = r * 0.14f)
            drawCircle(white, radius = r * 0.08f, center = Offset(c.x - r * 0.20f, c.y + r * 0.22f))
            drawCircle(white, radius = r * 0.08f, center = Offset(c.x + r * 0.20f, c.y + r * 0.22f))
        }

        else -> drawCircle(white, radius = r * 0.3f, center = c)
    }
}

/** A pentagon badge: gold with a coloured centre when unlocked, grey when still locked. */
@Composable
fun BadgeIcon(id: String, unlocked: Boolean, modifier: Modifier = Modifier.size(56.dp)) {
    val lockedGrey = Color(0xFF9CA3AF)
    val accent = badgeColor(id)
    Canvas(modifier) {
        val w = size.width
        val c = Offset(w / 2f, size.height / 2f + size.height * 0.02f)
        val outline = pentagonPath(c, w * 0.48f)
        if (unlocked) {
            drawPath(outline, Brush.linearGradient(listOf(GoldLight, GoldDark)))
            drawPath(outline, Color.White.copy(alpha = 0.7f), style = Stroke(width = w * 0.04f, join = StrokeJoin.Round))
        } else {
            drawPath(outline, lockedGrey.copy(alpha = 0.30f))
            drawPath(outline, lockedGrey.copy(alpha = 0.60f), style = Stroke(width = w * 0.04f, join = StrokeJoin.Round))
        }
        drawCircle(color = if (unlocked) accent else lockedGrey, radius = w * 0.27f, center = c)
        drawBadgeGlyph(id, c, w * 0.27f)
    }
}

/** A small trophy for the level markers and pop-ups. */
@Composable
fun TrophyGlyph(modifier: Modifier = Modifier, color: Color = Color(0xFF7C4A03)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        val cup = Path().apply {
            moveTo(w * 0.28f, h * 0.12f)
            lineTo(w * 0.72f, h * 0.12f)
            cubicTo(w * 0.72f, h * 0.50f, w * 0.62f, h * 0.60f, w * 0.50f, h * 0.60f)
            cubicTo(w * 0.38f, h * 0.60f, w * 0.28f, h * 0.50f, w * 0.28f, h * 0.12f)
            close()
        }
        drawPath(cup, color)
        drawArc(color, 90f, 180f, false, Offset(w * 0.06f, h * 0.16f), Size(w * 0.26f, h * 0.26f), style = stroke)
        drawArc(color, 270f, 180f, false, Offset(w * 0.68f, h * 0.16f), Size(w * 0.26f, h * 0.26f), style = stroke)
        drawLine(color, Offset(w * 0.50f, h * 0.60f), Offset(w * 0.50f, h * 0.78f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.32f, h * 0.88f), Offset(w * 0.68f, h * 0.88f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
    }
}

// ------------------------------------------------------------------ XP bar

/**
 * The level bar from the reference design: a blue-to-green fill, a round marker with a tick where
 * you are now, and a gold trophy at the end for the next level. The fill animates when it appears.
 */
@Composable
fun XpProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    var goal by remember { mutableStateOf(0f) }
    LaunchedEffect(fraction) { goal = fraction.coerceIn(0f, 1f) }
    val animated by animateFloatAsState(
        targetValue = goal,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "xpBar",
    )
    val track = MaterialTheme.colorScheme.surfaceVariant

    BoxWithConstraints(modifier.fillMaxWidth().height(40.dp)) {
        val marker = 30.dp
        val usable = maxOf(maxWidth - marker, 0.dp)

        Box(
            Modifier.align(Alignment.CenterStart).fillMaxWidth().height(12.dp)
                .clip(RoundedCornerShape(50)).background(track),
        )
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = 0.dp)
                .size(width = marker / 2 + usable * animated, height = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(XpBlue, XpTeal, XpGreen))),
        )
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = usable * animated).size(marker)
                .clip(CircleShape).background(XpTeal).border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(14.dp)) {
                val tick = Path().apply {
                    moveTo(size.width * 0.12f, size.height * 0.55f)
                    lineTo(size.width * 0.40f, size.height * 0.82f)
                    lineTo(size.width * 0.90f, size.height * 0.22f)
                }
                drawPath(tick, Color.White, style = Stroke(width = size.width * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Box(
            Modifier.align(Alignment.CenterEnd).size(marker).clip(CircleShape)
                .background(Brush.linearGradient(listOf(GoldLight, GoldDark)))
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            TrophyGlyph(Modifier.size(18.dp))
        }
    }
}

// ------------------------------------------------------------------ profile and level strip

/** Student profile: avatar, name, level and title, total XP. */
@Composable
fun ProfileCard(userName: String, level: LevelInfo, streak: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val name = userName.trim().ifEmpty { "Student" }

    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(scheme.primary, scheme.secondary)))
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(scheme.onPrimary.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.first().uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scheme.onPrimary)
                Text(
                    "Level ${level.level} · ${level.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onPrimary.copy(alpha = 0.92f),
                )
                Text(
                    "${level.xp} XP in total",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimary.copy(alpha = 0.85f),
                )
            }
            StreakPill(streak)
        }
    }
}

/** A slim card for the dashboard: your level and how far you are to the next one. Tap to open Rewards. */
@Composable
fun LevelStrip(level: LevelInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var goal by remember { mutableStateOf(0f) }
    LaunchedEffect(level.fraction) { goal = level.fraction }
    val animated by animateFloatAsState(
        targetValue = goal,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "levelStrip",
    )

    SoftCard(modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(XpBlue, XpTeal))),
                contentAlignment = Alignment.Center,
            ) {
                Text("${level.level}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text(
                        "Level ${level.level} · ${level.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${level.xpToNext} XP to go",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { animated },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = XpTeal,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}
