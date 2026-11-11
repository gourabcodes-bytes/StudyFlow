package com.studyflow.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Fades a card in while sliding it up a little. Pass the item's position so that
 * neighbouring cards arrive one after another (a short "stagger").
 */
@Composable
fun Modifier.appear(index: Int = 0): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(minOf(index, 8) * 45L)
        progress.animateTo(1f, tween(durationMillis = 380, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 40f
    }
}

/**
 * A slow 0 -> 1 -> 0 breathing value while [active], and a constant 0 otherwise.
 * It returns a State so that only the places that read it redraw (not the whole screen).
 */
@Composable
fun rememberPulse(active: Boolean): State<Float> {
    if (!active) return remember { mutableStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "pulse")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseValue",
    )
}

/** Stat card whose number counts up to its value. */
@Composable
fun AnimatedStatCard(target: Int, label: String, modifier: Modifier = Modifier) {
    var goal by remember { mutableStateOf(0) }
    LaunchedEffect(target) { goal = target }
    val shown by animateIntAsState(
        targetValue = goal,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "count",
    )
    StatCard("$shown", label, modifier)
}

private fun lerpOffset(a: Offset, b: Offset, t: Float): Offset =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

/** A round checkbox: the circle fills with a little bounce and the tick draws itself. */
@Composable
fun AnimatedCheck(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "check",
    )
    val fillColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tickColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Checkbox) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(26.dp)) {
            val p = progress.coerceIn(0f, 1f)
            val w = size.width
            // the bounce overshoots 1f a little, which makes the circle "pop"
            val radius = w / 2f * (0.88f + 0.12f * progress)
            val center = Offset(w / 2f, w / 2f)

            drawCircle(color = outlineColor.copy(alpha = 1f - p), radius = radius, center = center, style = Stroke(width = w * 0.09f))
            drawCircle(color = fillColor.copy(alpha = p), radius = radius, center = center)

            // the tick is two strokes drawn one after the other
            val a = Offset(w * 0.28f, w * 0.52f)
            val b = Offset(w * 0.44f, w * 0.68f)
            val c = Offset(w * 0.74f, w * 0.34f)
            val t1 = (p / 0.4f).coerceIn(0f, 1f)
            val t2 = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f)
            if (t1 > 0f) drawLine(tickColor, a, lerpOffset(a, b, t1), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
            if (t2 > 0f) drawLine(tickColor, b, lerpOffset(b, c, t2), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
        }
    }
}
