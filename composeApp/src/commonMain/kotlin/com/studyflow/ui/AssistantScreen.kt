package com.studyflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyflow.AppContainer
import com.studyflow.ai.AssistMode
import com.studyflow.ai.ChatMessage
import com.studyflow.ai.MdBlock
import com.studyflow.ai.MdSpan
import com.studyflow.ai.Role
import com.studyflow.ai.parseInline
import com.studyflow.ai.parseMarkdown
import com.studyflow.ai.rememberPdfPicker
import com.studyflow.ai.splitAnswers
import com.studyflow.model.Settings

@Composable
fun AssistantScreen(
    container: AppContainer,
    onOpenSettings: () -> Unit,
) {
    val controller = container.assistant
    val settings by container.settings.state.collectAsState()
    val messages by controller.messages.collectAsState()
    val busy by controller.busy.collectAsState()
    val attachment by controller.attachment.collectAsState()
    val notice by controller.notice.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf<AssistMode?>(null) }
    val listState = rememberLazyListState()

    val pickPdf = rememberPdfPicker { picked ->
        if (picked != null) {
            controller.attachPdf(picked.name, picked.bytes)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Study Assistant",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "AI-powered patient tutor for your coursework",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (messages.isNotEmpty()) {
                TextButton(onClick = { controller.newChat() }) {
                    Text("New Chat")
                }
            }
        }

        if (settings.apiKey.isBlank()) {
            SoftCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Set up your Anthropic API Key",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "To chat with the study assistant, add your Anthropic API key in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onOpenSettings, modifier = Modifier.align(Alignment.End)) {
                        Text("Open Settings")
                    }
                }
            }
        }

        if (notice != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = notice ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { controller.clearNotice() }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (messages.isEmpty() && settings.apiKey.isNotBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Ask anything about your studies",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Pick a mode below or type a topic to start.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatMessageRow(message = msg)
            }

            if (busy) {
                item {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Tutor is thinking...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Action / Input Section
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Quick Assist Mode Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = {
                                selectedMode = if (selectedMode == mode) null else mode
                            },
                            label = { Text(mode.label) },
                        )
                    }
                }

                // Attachment Pill
                if (attachment != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "📄 ${attachment?.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.clickable { controller.removeAttachment() },
                        )
                    }
                }

                // Input bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = pickPdf,
                        modifier = Modifier.height(56.dp),
                    ) {
                        Text("PDF")
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                if (attachment != null) "Ask about this PDF..."
                                else if (selectedMode != null) "Topic for ${selectedMode?.label}..."
                                else "Ask a question...",
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = false,
                        maxLines = 3,
                    )

                    Button(
                        onClick = {
                            val textToSend = inputText
                            val modeToSend = selectedMode
                            inputText = ""
                            selectedMode = null
                            controller.send(textToSend, modeToSend)
                        },
                        enabled = !busy && (inputText.isNotBlank() || attachment != null || selectedMode != null),
                        modifier = Modifier.height(56.dp),
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageRow(message: ChatMessage) {
    when (message.role) {
        Role.USER -> UserMessageBubble(message)
        Role.ASSISTANT -> AssistantMessageCard(message)
        Role.ERROR -> ErrorMessageCard(message)
    }
}

@Composable
private fun UserMessageBubble(message: ChatMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            if (message.pdfName != null) {
                Text(
                    text = "📄 ${message.pdfName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                )
            }
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun AssistantMessageCard(message: ChatMessage) {
    var showAnswers by remember { mutableStateOf(false) }
    val (questionsPart, answersPart) = remember(message.text) { splitAnswers(message.text) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        SelectionContainer {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MarkdownContent(source = questionsPart)

                if (answersPart != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAnswers = !showAnswers },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (showAnswers) "Hide Answers / Solutions" else "Show Answers / Solutions")
                    }

                    if (showAnswers) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        MarkdownContent(source = answersPart)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorMessageCard(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun MarkdownContent(source: String) {
    MarkdownText(source)
}

@Composable
fun AssistantSettingsCard(
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit,
) {
    SoftCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Study Assistant (AI)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Powered by Anthropic Claude API. Your key is stored on this device only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = settings.apiKey,
                onValueChange = { key -> onUpdate { it.copy(apiKey = key) } },
                label = { Text("Anthropic API Key") },
                placeholder = { Text("sk-ant-api...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = settings.studyProfile,
                onValueChange = { prof -> onUpdate { it.copy(studyProfile = prof) } },
                label = { Text("Study Profile / Major (optional)") },
                placeholder = { Text("e.g. 1st year ECE, CS major") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
