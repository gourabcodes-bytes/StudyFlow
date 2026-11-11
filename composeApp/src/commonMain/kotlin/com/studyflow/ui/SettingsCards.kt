package com.studyflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyflow.AppContainer
import com.studyflow.data.RestoreResult
import com.studyflow.data.createBackupText
import com.studyflow.data.parseBackup
import com.studyflow.logic.RemindChoices
import com.studyflow.model.Settings
import com.studyflow.notify.Notifier
import com.studyflow.platform.rememberFileSaver
import com.studyflow.platform.rememberNotificationPermission
import com.studyflow.platform.rememberTextFilePicker
import com.studyflow.util.formatDue
import com.studyflow.util.nowMillis
import com.studyflow.util.today

/** Switch reminders on or off, choose the default lead time, and send a test notification. */
@Composable
fun NotificationsCard(settings: Settings, notifier: Notifier, onUpdate: ((Settings) -> Settings) -> Unit) {
    val permission = rememberNotificationPermission()

    SoftCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Deadline and exam reminders", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "A notification before something is due",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.notificationsOn, onCheckedChange = { v -> onUpdate { it.copy(notificationsOn = v) } })
            }

            if (settings.notificationsOn && !permission.granted) {
                Text(
                    "Notifications are blocked for this app. Allow them so reminders can appear.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = permission.request) { Text("Allow notifications") }
            }

            if (settings.notificationsOn) {
                Text("Remind me", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // two rows so the chips fit on a narrow phone
                RemindChoices.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (minutes, label) ->
                            FilterChip(
                                selected = settings.defaultRemindMinutes == minutes,
                                onClick = { onUpdate { it.copy(defaultRemindMinutes = minutes) } },
                                label = { Text(label) },
                            )
                        }
                    }
                }
                Text(
                    "Each deadline can use its own time, or none, when you add or edit it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { notifier.showNow("StudyFlow", "Reminders are working.") }) {
                    Text("Send a test notification")
                }
            }
        }
    }
}

/** Offline status, plus saving everything to a file and restoring it. */
@Composable
fun BackupCard(container: AppContainer, settings: Settings) {
    val data by container.repository.data.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }

    val save = rememberFileSaver { ok ->
        if (ok) {
            container.settings.update { it.copy(lastBackupAt = nowMillis()) }
            message = "Backup saved. Keep the file somewhere safe, such as Google Drive or iCloud."
        } else {
            message = "The backup was cancelled or could not be saved."
        }
    }

    val pick = rememberTextFilePicker { text ->
        message = if (text == null) {
            "Nothing was restored."
        } else {
            when (val result = parseBackup(text)) {
                is RestoreResult.Bad -> result.message
                is RestoreResult.Ok -> {
                    val merged = container.repository.restore(result.data)
                    "Restored. You now have ${merged.tasks.count { !it.deleted }} deadlines, " +
                        "${merged.classes.count { !it.deleted }} classes and ${merged.notes.count { !it.deleted }} notes."
                }
            }
        }
    }

    SoftCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Offline and backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Works without internet. Saved on this device: ${data.tasks.count { !it.deleted }} deadlines, " +
                    "${data.classes.count { !it.deleted }} classes, ${data.notes.count { !it.deleted }} notes and " +
                    "${data.sessions.count { !it.deleted }} focus sessions.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                if (settings.lastBackupAt == 0L) "No backup yet." else "Last backup: ${formatDue(settings.lastBackupAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    save("studyflow-backup-${today()}.json", createBackupText(container.repository.snapshot(), nowMillis()))
                }) { Text("Save backup") }
                OutlinedButton(onClick = { pick() }) { Text("Restore") }
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                "Cloud backup: save the file into Google Drive, iCloud or any folder that syncs. " +
                    "Restoring adds the backup to what is here and never replaces newer changes. " +
                    "To keep your phone and computer in step, use Sync below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
