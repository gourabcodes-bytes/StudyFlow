package com.studyflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.studyflow.logic.overlaps
import com.studyflow.model.ClassSlot
import com.studyflow.model.Priority
import com.studyflow.model.StudyTask
import com.studyflow.util.DAY_SHORT
import com.studyflow.util.formatDate
import com.studyflow.util.formatMinute
import com.studyflow.util.minuteOfDay
import com.studyflow.util.newId
import com.studyflow.util.toLocal
import com.studyflow.util.toMillis
import com.studyflow.util.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private const val LAST_MINUTE_OF_DAY = 23 * 60 + 59

@Composable
fun ClassDialog(
    initial: ClassSlot?,
    defaultDay: Int,
    allClasses: List<ClassSlot>,
    onSave: (ClassSlot) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val id = remember { initial?.id ?: newId() }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var day by remember { mutableStateOf(initial?.dayOfWeek ?: defaultDay) }
    var startMin by remember { mutableStateOf(initial?.startMinute ?: 9 * 60) }
    var endMin by remember { mutableStateOf(initial?.endMinute ?: 10 * 60) }
    var color by remember { mutableStateOf(initial?.colorIndex ?: 0) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    val problem: String? = when {
        title.isBlank() -> "Give the class a name"
        endMin <= startMin -> "The class must end after it starts"
        else -> null
    }
    val candidate: ClassSlot? =
        if (problem == null) {
            ClassSlot(
                id = id,
                title = title.trim(),
                location = location.trim(),
                dayOfWeek = day,
                startMinute = startMin,
                endMinute = endMin,
                colorIndex = color,
            )
        } else {
            null
        }
    val clash = candidate != null && allClasses.any { overlaps(candidate, it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New class" else "Edit class") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Room / location (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (d in 1..7) {
                        FilterChip(selected = d == day, onClick = { day = d }, label = { Text(DAY_SHORT[d - 1]) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PickerField("Starts", formatMinute(startMin), onClick = { pickingStart = true }, modifier = Modifier.weight(1f))
                    PickerField("Ends", formatMinute(endMin), onClick = { pickingEnd = true }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ClassPalette.forEachIndexed { index, c ->
                        Box(
                            Modifier
                                .size(if (index == color) 34.dp else 28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { color = index },
                        )
                    }
                }
                if (problem != null) {
                    Text(problem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else if (clash) {
                    Text(
                        "Heads up: this overlaps another class (you can still save it).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = candidate != null, onClick = { candidate?.let(onSave) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    if (pickingStart) {
        TimePickerSheet(
            title = "Class starts at",
            initialMinute = startMin,
            onPick = { picked ->
                startMin = picked
                if (endMin <= picked) endMin = minOf(picked + 60, LAST_MINUTE_OF_DAY)
                pickingStart = false
            },
            onDismiss = { pickingStart = false },
        )
    }
    if (pickingEnd) {
        TimePickerSheet(
            title = "Class ends at",
            initialMinute = endMin,
            onPick = { picked ->
                endMin = picked
                pickingEnd = false
            },
            onDismiss = { pickingEnd = false },
        )
    }
}

@Composable
fun TaskDialog(
    initial: StudyTask?,
    onSave: (StudyTask) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    courseSuggestions: List<String> = emptyList(),
) {
    val id = remember { initial?.id ?: newId() }
    val initialDue = remember { initial?.dueAt?.toLocal() }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var course by remember { mutableStateOf(initial?.course ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var date by remember { mutableStateOf<LocalDate>(initialDue?.date ?: today().plus(1, DateTimeUnit.DAY)) }
    var minute by remember { mutableStateOf(if (initialDue != null) minuteOfDay(initialDue) else 18 * 60) }
    var priority by remember { mutableStateOf(initial?.priority ?: Priority.MEDIUM) }
    var isExam by remember { mutableStateOf(initial?.isExam ?: false) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }

    val problem: String? = if (title.isBlank()) "Give the deadline a title" else null
    val candidate: StudyTask? =
        if (problem == null) {
            StudyTask(
                id = id,
                title = title.trim(),
                course = course.trim(),
                notes = notes.trim(),
                dueAt = toMillis(date, minute),
                priority = priority,
                isExam = isExam,
                done = initial?.done ?: false,
            )
        } else {
            null
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New deadline" else "Edit deadline") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What is due?") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = !isExam, onClick = { isExam = false }, label = { Text("Assignment") })
                    FilterChip(
                        selected = isExam,
                        onClick = {
                            isExam = true
                            if (priority == Priority.LOW) priority = Priority.HIGH
                        },
                        label = { Text("Exam") },
                    )
                }
                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text("Subject (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (courseSuggestions.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        courseSuggestions.take(6).forEach { name ->
                            AssistChip(onClick = { course = name }, label = { Text(name) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PickerField("Due date", formatDate(date), onClick = { pickingDate = true }, modifier = Modifier.weight(1.6f))
                    PickerField("Time", formatMinute(minute), onClick = { pickingTime = true }, modifier = Modifier.weight(1f))
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Today" to 0, "Tomorrow" to 1, "In 3 days" to 3, "Next week" to 7).forEach { (label, days) ->
                        AssistChip(
                            onClick = { date = today().plus(days, DateTimeUnit.DAY) },
                            label = { Text(label) },
                        )
                    }
                }
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = p == priority,
                            onClick = { priority = p },
                            label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                if (problem != null) {
                    Text(problem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = candidate != null, onClick = { candidate?.let(onSave) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    if (pickingDate) {
        DatePickerSheet(
            initial = date,
            onPick = {
                date = it
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
    if (pickingTime) {
        TimePickerSheet(
            title = "Due at",
            initialMinute = minute,
            onPick = {
                minute = it
                pickingTime = false
            },
            onDismiss = { pickingTime = false },
        )
    }
}
