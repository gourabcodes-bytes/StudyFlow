package com.studyflow.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyflow.logic.notePreview
import com.studyflow.model.AppData
import com.studyflow.model.Note
import com.studyflow.util.formatDate
import com.studyflow.util.newId
import com.studyflow.util.nowMillis
import com.studyflow.util.toLocal

/** Saved notes: search, read, edit, delete. Everything is stored on the device, so it works offline. */
@Composable
fun NotesScreen(data: AppData, onSave: (Note) -> Unit, onDelete: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<Note?>(null) }
    var creating by remember { mutableStateOf(false) }

    val notes = data.notes
        .filter { !it.deleted }
        .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true) }
        .sortedByDescending { it.updatedAt }

    Box(Modifier.fillMaxSize()) {
        ScreenColumn {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (notes.isEmpty()) {
                item {
                    EmptyHint(
                        if (query.isBlank()) "No notes yet. Tap + to write one, or use \"Save as note\" under an assistant answer."
                        else "No notes match your search.",
                    )
                }
            } else {
                itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
                    SoftCard(Modifier.animateItem().appear(index).clickable { open = note }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                note.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                notePreview(note.body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                formatDate(note.updatedAt.toLocal().date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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
        NoteDialog(
            initial = null,
            onSave = {
                onSave(it)
                creating = false
            },
            onDelete = null,
            onDismiss = { creating = false },
        )
    }
    open?.let { note ->
        NoteDialog(
            initial = note,
            onSave = {
                onSave(it)
                open = null
            },
            onDelete = {
                onDelete(note.id)
                open = null
            },
            onDismiss = { open = null },
        )
    }
}

/** Reading view for an existing note (with Edit), or the editor for a new one. */
@Composable
private fun NoteDialog(
    initial: Note?,
    onSave: (Note) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val id = remember { initial?.id ?: newId() }
    var editing by remember { mutableStateOf(initial == null) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }
    val canSave = title.isNotBlank() || body.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing) (if (initial == null) "New note" else "Edit note") else title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (editing) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Note") },
                        minLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    MarkdownText(body)
                }
            }
        },
        confirmButton = {
            if (editing) {
                TextButton(
                    enabled = canSave,
                    onClick = {
                        val now = nowMillis()
                        onSave(
                            Note(
                                id = id,
                                title = title.trim().ifEmpty { "Untitled note" },
                                body = body.trim(),
                                createdAt = initial?.createdAt ?: now,
                                updatedAt = now,
                            ),
                        )
                    },
                ) { Text("Save") }
            } else {
                TextButton(onClick = { editing = true }) { Text("Edit") }
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text(if (editing) "Cancel" else "Close") }
            }
        },
    )
}
