package com.studyflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyflow.logic.groupActive
import com.studyflow.model.AppData
import com.studyflow.model.StudyTask

private val FILTERS = listOf("Active", "Done", "All")

@Composable
fun DeadlinesScreen(
    data: AppData,
    now: Long,
    onSave: (StudyTask) -> Unit,
    onToggle: (StudyTask, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(0) }
    var editing by remember { mutableStateOf<StudyTask?>(null) }
    var creating by remember { mutableStateOf(false) }

    val live = data.tasks.filter { !it.deleted }
    val groups = groupActive(data.tasks, now)
    val done = live.filter { it.done }.sortedByDescending { it.dueAt }
    val courses = (data.classes.filter { !it.deleted }.map { it.title } + live.map { it.course })
        .filter { it.isNotBlank() }
        .distinct()

    Box(Modifier.fillMaxSize()) {
        ScreenColumn {
            item {
                ScreenHeader(
                    "Deadlines",
                    "${live.count { !it.done }} open · ${done.size} finished",
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FILTERS.forEachIndexed { index, label ->
                        FilterChip(selected = filter == index, onClick = { filter = index }, label = { Text(label) })
                    }
                }
            }

            val showActive = filter != 1
            val showDone = filter != 0

            if (showActive) {
                if (groups.overdue.isNotEmpty()) {
                    item { SectionLabel("Overdue") }
                    itemsIndexed(groups.overdue, key = { _, task -> "o-" + task.id }) { index, task ->
                        TaskRow(
                            task,
                            now,
                            onToggle = { onToggle(task, it) },
                            onClick = { editing = task },
                            modifier = Modifier.animateItem().appear(index),
                        )
                    }
                }
                if (groups.thisWeek.isNotEmpty()) {
                    item { SectionLabel("Next 7 days") }
                    itemsIndexed(groups.thisWeek, key = { _, task -> "w-" + task.id }) { index, task ->
                        TaskRow(
                            task,
                            now,
                            onToggle = { onToggle(task, it) },
                            onClick = { editing = task },
                            modifier = Modifier.animateItem().appear(index),
                        )
                    }
                }
                if (groups.later.isNotEmpty()) {
                    item { SectionLabel("Later") }
                    itemsIndexed(groups.later, key = { _, task -> "l-" + task.id }) { index, task ->
                        TaskRow(
                            task,
                            now,
                            onToggle = { onToggle(task, it) },
                            onClick = { editing = task },
                            modifier = Modifier.animateItem().appear(index),
                        )
                    }
                }
                if (filter == 0 && groups.overdue.isEmpty() && groups.thisWeek.isEmpty() && groups.later.isEmpty()) {
                    item { EmptyHint("Nothing pending. Tap + to add a deadline.") }
                }
            }
            if (showDone) {
                if (done.isNotEmpty()) {
                    item { SectionLabel("Finished") }
                    itemsIndexed(done, key = { _, task -> "d-" + task.id }) { index, task ->
                        TaskRow(
                            task,
                            now,
                            onToggle = { onToggle(task, it) },
                            onClick = { editing = task },
                            modifier = Modifier.animateItem().appear(index),
                        )
                    }
                } else if (filter == 1) {
                    item { EmptyHint("Finished deadlines will show up here.") }
                }
            }
        }

        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Text("+", fontSize = 28.sp)
        }
    }

    if (creating) {
        TaskDialog(
            initial = null,
            courseSuggestions = courses,
            onSave = {
                onSave(it)
                creating = false
            },
            onDelete = null,
            onDismiss = { creating = false },
        )
    }

    editing?.let { current ->
        TaskDialog(
            initial = current,
            courseSuggestions = courses,
            onSave = {
                onSave(it)
                editing = null
            },
            onDelete = {
                onDelete(current.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}
