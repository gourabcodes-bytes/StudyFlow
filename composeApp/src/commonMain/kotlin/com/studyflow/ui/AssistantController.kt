package com.studyflow.ai

import com.studyflow.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val MAX_PDF_BYTES = 25 * 1024 * 1024

enum class Role { USER, ASSISTANT, ERROR }

data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
    /** Name of the PDF that was sent with this message, if any. */
    val pdfName: String? = null,
)

data class PdfAttachment(val name: String, val base64: String, val sizeBytes: Int)

@OptIn(ExperimentalEncodingApi::class)
private fun encodeBase64(bytes: ByteArray): String = Base64.Default.encode(bytes)

/**
 * Holds the conversation with the study assistant. The screen shows [messages]; [history] is the
 * exact conversation sent to the AI (it includes the PDF, so follow-up questions still know it).
 */
class AssistantController(
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val client = AssistantClient()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _attachment = MutableStateFlow<PdfAttachment?>(null)
    val attachment: StateFlow<PdfAttachment?> = _attachment.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private var history: List<ApiMessage> = emptyList()
    private var nextId = 1L

    /** Bumped by [newChat] so that an answer arriving for an old chat is thrown away. */
    private var chatVersion = 0

    fun attachPdf(name: String, bytes: ByteArray) {
        if (bytes.isEmpty()) {
            _notice.value = "That file looks empty. Choose another PDF."
            return
        }
        if (bytes.size > MAX_PDF_BYTES) {
            _notice.value = "That PDF is larger than 25 MB. Choose a smaller one or split it."
            return
        }
        _attachment.value = PdfAttachment(name, encodeBase64(bytes), bytes.size)
        _notice.value = null
    }

    fun removeAttachment() {
        _attachment.value = null
    }

    fun clearNotice() {
        _notice.value = null
    }

    fun newChat() {
        chatVersion++
        history = emptyList()
        _messages.value = emptyList()
        _attachment.value = null
        _notice.value = null
        _busy.value = false
    }

    fun send(text: String, mode: AssistMode?) {
        if (_busy.value) return
        val pdf = _attachment.value
        val prompt = buildUserPrompt(mode, text, hasPdf = pdf != null)
        if (prompt.isEmpty()) return

        val s = settings.state.value
        if (s.apiKey.isBlank()) {
            add(Role.ERROR, "Add your API key in Settings to use the assistant.")
            return
        }

        val typed = text.trim()
        val shown = when {
            mode != null && typed.isNotEmpty() -> "${mode.label}: $typed"
            mode != null -> mode.label
            typed.isNotEmpty() -> typed
            else -> "Explain my PDF"
        }
        add(Role.USER, shown, pdf?.name)
        _attachment.value = null
        _busy.value = true

        val blocks = ArrayList<ApiBlock>()
        if (pdf != null) {
            // the PDF goes first, as the documentation recommends, and is marked for caching
            blocks += ApiBlock(
                type = "document",
                source = ApiSource(type = "base64", mediaType = "application/pdf", data = pdf.base64),
                cacheControl = ApiCacheControl("ephemeral"),
            )
        }
        blocks += ApiBlock(type = "text", text = prompt)
        val turn = ApiMessage(role = "user", content = blocks)
        val request = history + turn
        val version = chatVersion

        scope.launch {
            val result = client.ask(s.apiKey, s.aiModel, buildSystemPrompt(s.studyProfile), request)
            if (version != chatVersion) return@launch // the student started a new chat meanwhile
            when (result) {
                is AiResult.Reply -> {
                    history = request + ApiMessage(role = "assistant", content = listOf(ApiBlock(type = "text", text = result.text)))
                    val note = if (result.cutOff) "\n\n(The answer was cut short. Ask me to continue.)" else ""
                    add(Role.ASSISTANT, result.text + note)
                }

                is AiResult.Failure -> {
                    add(Role.ERROR, result.message)
                    // give the PDF back so the student can simply press Send again
                    if (pdf != null && _attachment.value == null) _attachment.value = pdf
                }
            }
            _busy.value = false
        }
    }

    private fun add(role: Role, text: String, pdfName: String? = null) {
        val message = ChatMessage(nextId++, role, text, pdfName)
        _messages.value = _messages.value + message
    }

    fun close() {
        client.close()
    }
}
