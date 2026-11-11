package com.studyflow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studyflow.logic.BadgeState
import com.studyflow.logic.ChallengeState
import com.studyflow.logic.LevelInfo
import com.studyflow.logic.epochDay
import com.studyflow.logic.gameSnapshot
import com.studyflow.logic.weekBounds
import com.studyflow.model.AppData
import com.studyflow.model.XpEvent
import com.studyflow.util.DAY_MS

/** The Rewards screen: profile and level, XP bar, badges, weekly challenges and recent XP. */
@Composable
fun AchievementsScreen(data: AppData, now: Long, userName: String) {
    val (weekStart, weekEnd) = weekBounds(now)
    val snapshot = gameSnapshot(data, now, weekStart, weekEnd, ::epochDay)
    val daysLeft = ((weekEnd - now + DAY_MS - 1) / DAY_MS).toInt().coerceAtLeast(1)

    var showXpTips by remember { mutableStateOf(false) }
    var showAllBadges by remember { mutableStateOf(false) }

    ScreenColumn {
        item { ScreenHeader("Your Achievements", "Track your progress, earn rewards, and stay motivated!") }
        item { ProfileCard(userName, snapshot.level, snapshot.streak, Modifier.appear(0)) }
        item { XpCard(snapshot.level, showXpTips, onToggleTips = { showXpTips = !showXpTips }, Modifier.appear(1)) }
        item { BadgesCard(snapshot.badges, showAllBadges, onToggle = { showAllBadges = !showAllBadges }, Modifier.appear(2)) }
        item { ChallengesCard(snapshot.challenges, daysLeft, Modifier.appear(3)) }
        item { RecentXpCard(snapshot.recent, Modifier.appear(4)) }
    }
}

@Composable
private fun PillButton(text: String, brush: Brush, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(brush)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun XpCard(level: LevelInfo, showTips: Boolean, onToggleTips: () -> Unit, modifier: Modifier = Modifier) {
    SoftCard(modifier) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("XP Progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Level ${level.level}", style = MaterialTheme.typography.labelLarge, color = XpBlue, fontWeight = FontWeight.SemiBold)
                Text("Level ${level.level + 1}", style = MaterialTheme.typography.labelLarge, color = XpGreen, fontWeight = FontWeight.SemiBold)
            }
            XpProgressBar(level.fraction)
            Text(
                "${level.xp} XP / ${level.nextXp} XP to Level ${level.level + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PillButton(
                    text = if (showTips) "Hide tips" else "Earn More XP",
                    brush = Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFFBBF24))),
                    onClick = onToggleTips,
                )
            }
            AnimatedVisibility(visible = showTips) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    listOf(
                        "Finish a deadline: +10 to +20 XP (exams +40)",
                        "Finish a focus session: 1 XP per minute (5 to 60)",
                        "First focus session of the day: +10 XP bonus",
                        "Unlock a badge: +25 XP",
                        "Complete a weekly challenge: +50 to +70 XP",
                    ).forEach {
                        Text("•  $it", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgesCard(badges: List<BadgeState>, showAll: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val unlocked = badges.count { it.unlocked }
    val ordered = badges.sortedByDescending { it.unlocked }

    SoftCard(modifier) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Badges  ·  $unlocked of ${badges.size} unlocked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (showAll) {
                ordered.chunked(3).forEach { rowBadges ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowBadges.forEach { BadgeTile(it, Modifier.weight(1f)) }
                        repeat(3 - rowBadges.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ordered.take(4).forEach { BadgeTile(it, Modifier.width(112.dp)) }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PillButton(
                    text = if (showAll) "Show less" else "View All Badges",
                    brush = Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB))),
                    onClick = onToggle,
                )
            }
        }
    }
}

@Composable
private fun BadgeTile(state: BadgeState, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BadgeIcon(state.def.id, state.unlocked, Modifier.size(58.dp))
        Text(
            state.def.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            state.def.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            minLines = 3,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (state.unlocked) {
            Text("Unlocked", style = MaterialTheme.typography.labelMedium, color = XpGreen, fontWeight = FontWeight.SemiBold)
        } else {
            val shown = minOf(state.progress, state.def.target)
            LinearProgressIndicator(
                progress = { shown.toFloat() / state.def.target },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = XpBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text("$shown / ${state.def.target}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChallengesCard(challenges: List<ChallengeState>, daysLeft: Int, modifier: Modifier = Modifier) {
    SoftCard(modifier) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Weekly Challenges",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (daysLeft == 1) "Ends today" else "$daysLeft days left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            challenges.forEach { ChallengeRow(it) }
        }
    }
}

@Composable
private fun ChallengeRow(state: ChallengeState) {
    val target = state.def.target
    val shown = if (state.done) target else minOf(state.progress, target)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(state.def.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    state.def.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.done) {
                Text("Completed", style = MaterialTheme.typography.labelLarge, color = XpGreen, fontWeight = FontWeight.Bold)
            } else {
                Text("+${state.def.xp} XP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        LinearProgressIndicator(
            progress = { shown.toFloat() / target },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = if (state.done) XpGreen else XpBlue,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text("$shown / $target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecentXpCard(recent: List<XpEvent>, modifier: Modifier = Modifier) {
    SoftCard(modifier) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Recent XP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (recent.isEmpty()) {
                Text(
                    "Finish a deadline or a focus session to earn your first XP.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                for (event in recent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            event.label,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        Text(
                            "+${event.amount} XP",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
