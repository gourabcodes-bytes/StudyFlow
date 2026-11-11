package com.studyflow.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyflow.logic.classesOn
import com.studyflow.logic.overlaps
import com.studyflow.model.AppData
import com.studyflow.model.ClassSlot
import com.studyflow.util.DAY_LONG
import com.studyflow.util.DAY_SHORT
import com.studyflow.util.isoDay
import com.studyflow.util.nowMillis
import com.studyflow.util.toLocal

@Composable
fun TimetableScreen(
    data: AppData,
    onSave: (ClassSlot) -> Unit,
    onDelete: (String) -> Unit,
) {
    var selectedDay by rememberSaveable { mutableStateOf(isoDay(nowMillis().toLocal())) }
    var editing by remember { mutableStateOf<ClassSlot?>(null) }
    var creating by remember { mutableStateOf(false) }

    val list = classesOn(data.classes, selectedDay)

    Box(Modifier.fillMaxSize()) {
        ScreenColumn {
            item { ScreenHeader("Timetable", "Your week at a glance") }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (d in 1..7) {
                        val count = classesOn(data.classes, d).size
                        FilterChip(
                            selected = d == selectedDay,
                            onClick = { selectedDay = d },
                            label = { Text(if (count > 0) "${DAY_SHORT[d - 1]} · $count" else DAY_SHORT[d - 1]) },
                        )
                    }
                }
            }
            if (list.isEmpty()) {
                item { EmptyHint("No classes on ${DAY_LONG[selectedDay - 1]}. Tap + to add one.") }
            } else {
                itemsIndexed(list, key = { _, slot -> slot.id }) { index, slot ->
                    ClassCard(
                        slot = slot,
                        conflict = list.any { other -> overlaps(slot, other) },
                        onClick = { editing = slot },
                        modifier = Modifier.animateItem().appear(index),
                    )
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
        ClassDialog(
            initial = null,
            defaultDay = selectedDay,
            allClasses = data.classes,
            onSave = {
                onSave(it)
                selectedDay = it.dayOfWeek
                creating = false
            },
            onDelete = null,
            onDismiss = { creating = false },
        )
    }

    editing?.let { current ->
        ClassDialog(
            initial = current,
            defaultDay = current.dayOfWeek,
            allClasses = data.classes,
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
