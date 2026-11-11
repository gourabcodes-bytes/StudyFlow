package com.studyflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.studyflow.AppContainer
import com.studyflow.util.humanDuration
import com.studyflow.util.nowMillis
import kotlinx.coroutines.launch

@Composable
fun SyncScreen(container: AppContainer, now: Long) {
    val settings by container.settings.state.collectAsState()
    val status by container.sync.status.collectAsState()
    val scope = rememberCoroutineScope()
    val host = container.syncHost

    ScreenColumn {
        item { ScreenHeader("Settings", "Make it yours and keep your devices in step") }

        item {
            SoftCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = settings.userName,
                        onValueChange = { v -> container.settings.update { it.copy(userName = v.take(30)) } },
                        label = { Text("Shown in the greeting on Today") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item {
            AppearanceCard(
                themeMode = settings.themeMode,
                accentIndex = settings.accentIndex,
                onThemeMode = { v -> container.settings.update { it.copy(themeMode = v) } },
                onAccent = { v -> container.settings.update { it.copy(accentIndex = v) } },
            )
        }

        item {
            AssistantSettingsCard(
                settings = settings,
                onUpdate = { block -> container.settings.update(block) },
            )
        }

        item { SectionLabel("Sync · no account, no cloud") }

        if (host != null) {
            item {
                val running by host.running.collectAsState()
                val addresses by host.addresses.collectAsState()
                val error by host.lastError.collectAsState()
                SoftCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("This computer is the sync hub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (running) "Listening — your phone can connect now" else "Switch on, then enter the address on your phone",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = running, onCheckedChange = { if (it) host.start() else host.stop() })
                        }
                        Text("Pairing code", style = MaterialTheme.typography.labelLarge)
                        Text(
                            settings.hostCode,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (running) {
                            Text("Address to type on your phone", style = MaterialTheme.typography.labelLarge)
                            if (addresses.isEmpty()) {
                                Text("No network address found. Connect this computer to Wi-Fi.", style = MaterialTheme.typography.bodyMedium)
                            }
                            addresses.forEach { Text(it, style = MaterialTheme.typography.titleMedium) }
                        }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }

        item {
            SoftCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sync with a hub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = settings.syncHost,
                        onValueChange = { v -> container.settings.update { it.copy(syncHost = v) } },
                        label = { Text("Hub address (e.g. 192.168.1.20)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = settings.syncCode,
                        onValueChange = { v -> container.settings.update { it.copy(syncCode = v) } },
                        label = { Text("Pairing code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            enabled = !status.busy,
                            onClick = { scope.launch { container.sync.syncNow() } },
                        ) { Text(if (status.busy) "Syncing…" else "Sync now") }
                        Text(
                            if (settings.lastSyncAt == 0L) "Never synced" else "Last sync ${humanDuration(now - settings.lastSyncAt)} ago",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        status.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (status.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Sync automatically", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Every 2 minutes while the app is open",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = settings.autoSync, onCheckedChange = { v -> container.settings.update { it.copy(autoSync = v) } })
                    }
                }
            }
        }

        item {
            SoftCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("How it works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Everything is stored on each device, so StudyFlow works offline. When you sync, " +
                                "both devices exchange their data over your local Wi-Fi and keep the newest version " +
                                "of every class and deadline. Deletions sync too. The pairing code stops other " +
                                "devices on the network from reading or changing your data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}