package com.studyflow.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyflow.ai.MdBlock
import com.studyflow.ai.parseInline
import com.studyflow.ai.parseMarkdown
import com.studyflow.model.ClassSlot
import com.studyflow.model.Priority
import com.studyflow.model.StudyTask
import com.studyflow.util.DAY_MS
import com.studyflow.util.formatDue
import com.studyflow.util.formatMinute
import com.studyflow.util.relativeDue

/** Centres content and caps its width so the desktop layout stays readable. */
@Composable
fun ScreenColumn(content: LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    Column(Modifier.padding(bottom = 6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
fun EmptyHint(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            text,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun ClassCard(
    slot: ClassSlot,
    highlight: Boolean = false,
    conflict: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val accent = ClassPalette[slot.colorIndex.coerceIn(0, ClassPalette.lastIndex)]
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(accent))
            Column(Modifier.padding(14.dp).weight(1f)) {
                Text(slot.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val where = if (slot.location.isBlank()) "" else "  ·  ${slot.location}"
                Text(
                    "${formatMinute(slot.startMinute)} – ${formatMinute(slot.endMinute)}$where",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (conflict) {
                    Text("Overlaps another class", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun priorityColor(p: Priority): Color = when (p) {
    Priority.HIGH -> Color(0xFFDC2626)
    Priority.MEDIUM -> Color(0xFFF59E0B)
    Priority.LOW -> Color(0xFF22C55E)
}

@Composable
fun TaskRow(
    task: StudyTask,
    now: Long,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var checked by remember(task.id, task.done) { mutableStateOf(task.done) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (checked) 0.55f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "doneFade",
    )
    val overdue = !checked && task.dueAt < now

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedCheck(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onToggle(it)
                },
            )
            Column(Modifier.weight(1f).padding(end = 8.dp).alpha(contentAlpha)) {
                if (task.isExam) ExamTag()
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                )
                val course = if (task.course.isBlank()) "" else "${task.course}  ·  "
                Text(
                    "$course${formatDue(task.dueAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!checked) {
                    Text(
                        relativeDue(task.dueAt, now),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (overdue) MaterialTheme.colorScheme.error else if (task.dueAt - now < DAY_MS) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Box(Modifier.padding(end = 10.dp).size(12.dp).clip(CircleShape).background(priorityColor(task.priority)))
        }
    }
}

@Composable
private fun ExamTag() {
    Text(
        "EXAM",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .padding(bottom = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun Spacer16() {
    Spacer(Modifier.height(16.dp))
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        content()
    }
}

@Composable
fun MarkdownText(source: String, modifier: Modifier = Modifier) {
    val blocks = remember(source) { parseMarkdown(source) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    Text(
                        text = block.text,
                        style = style,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                is MdBlock.Paragraph -> {
                    Text(text = renderMarkdownInline(block.text), style = MaterialTheme.typography.bodyMedium)
                }

                is MdBlock.Bullet -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = (block.indent * 16).dp),
                    ) {
                        Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = renderMarkdownInline(block.text), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                is MdBlock.Numbered -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = (block.indent * 16).dp),
                    ) {
                        Text("${block.number}.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = renderMarkdownInline(block.text), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                is MdBlock.Code -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = block.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                MdBlock.Divider -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun renderMarkdownInline(text: String) = buildAnnotatedString {
    val spans = parseInline(text)
    spans.forEach { span ->
        when {
            span.code -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.Black.copy(alpha = 0.08f),
                    ),
                ) {
                    append(span.text)
                }
            }
            span.bold -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(span.text)
                }
            }
            span.italic -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(span.text)
                }
            }
            else -> append(span.text)
        }
    }
}
