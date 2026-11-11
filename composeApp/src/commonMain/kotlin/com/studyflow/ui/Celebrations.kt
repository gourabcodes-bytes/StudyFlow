package com.studyflow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studyflow.data.StudyRepository
import com.studyflow.logic.Celebration
import com.studyflow.logic.CelebrationKind
import kotlinx.coroutines.delay

/**
 * Shows what the student just earned: a small "+25 XP" banner that fades away on its own, and a
 * pop-up with an animated icon for badges, finished challenges and new levels.
 */
@Composable
fun CelebrationHost(repository: StudyRepository) {
    val pending by repository.celebrations.collectAsState()
    val toast = pending.firstOrNull { it.kind == CelebrationKind.XP }
    val dialog = pending.firstOrNull { it.kind != CelebrationKind.XP }

    // keep the last banner around while it slides out
    var lastToast by remember { mutableStateOf<Celebration?>(null) }
    if (toast != null) lastToast = toast

    val toastId = toast?.id
    LaunchedEffect(toastId) {
        if (toastId != null) {
            delay(2400)
            repository.dismissCelebration(toastId)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = toast != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            lastToast?.let { XpToast(it) }
        }
    }

    if (dialog != null) {
        AlertDialog(
            onDismissRequest = { repository.dismissCelebration(dialog.id) },
            icon = { CelebrationIcon(dialog) },
            title = { Text(dialog.title, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
            text = { Text(dialog.subtitle, textAlign = TextAlign.Center) },
            confirmButton = {
                Button(onClick = { repository.dismissCelebration(dialog.id) }) { Text("Awesome!") }
            },
        )
    }
}

@Composable
private fun XpToast(celebration: Celebration) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFFBBF24))))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(celebration.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(celebration.subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.95f), maxLines = 1)
    }
}

/** The icon pops in with a little bounce. */
@Composable
private fun CelebrationIcon(celebration: Celebration) {
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(celebration.id) {
        scale.snapTo(0.4f)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(
        Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        contentAlignment = Alignment.Center,
    ) {
        val badgeId = celebration.badgeId
        if (celebration.kind == CelebrationKind.BADGE && badgeId != null) {
            BadgeIcon(badgeId, unlocked = true, modifier = Modifier.size(84.dp))
        } else {
            Box(
                Modifier.size(76.dp).clip(CircleShape).background(Brush.linearGradient(listOf(GoldLight, GoldDark))),
                contentAlignment = Alignment.Center,
            ) {
                TrophyGlyph(Modifier.size(44.dp))
            }
        }
    }
}
