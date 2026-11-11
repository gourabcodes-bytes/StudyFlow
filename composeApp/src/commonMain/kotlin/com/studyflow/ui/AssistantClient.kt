package com.studyflow.ai

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

private const val API_URL = "https://api.anthropic.com/v1/messages"
private const val API_VERSION = "2023-06-01"
private const val MAX_ANSWER_TOKENS = 2048

sealed interface AiResult {
    data class Reply(val text: String, val cutOff: Boolean) : AiResult
    data class Failure(val message: String) : AiResult
}

/** Sends the conversation to Anthropic's Messages API and returns the tutor's answer. */
class AssistantClient {
    private val client = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 180_000
            socketTimeoutMillis = 180_000
        }
    }

    suspend fun ask(apiKey: String, model: String, system: String, messages: List<ApiMessage>): AiResult {
        val body = ApiJson.encodeToString(
            ApiRequest.serializer(),
            ApiRequest(model = model, maxTokens = MAX_ANSWER_TOKENS, system = system, messages = messages),
        )
        return try {
            val response = client.post(API_URL) {
                header("x-api-key", apiKey.trim())
                header("anthropic-version", API_VERSION)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                AiResult.Failure(friendlyError(response.status.value, text))
            } else {
                val parsed = ApiJson.decodeFromString(ApiResponse.serializer(), text)
                val reply = parsed.content.filter { it.type == "text" }.mapNotNull { it.text }.joinToString("")
                if (reply.isBlank()) {
                    AiResult.Failure("The assistant sent an empty answer. Please try again.")
                } else {
                    AiResult.Reply(reply, cutOff = parsed.stopReason == "max_tokens")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AiResult.Failure("Could not reach the AI service. Check your internet connection and try again. (${e.message ?: "unknown error"})")
        }
    }

    fun close() {
        client.close()
    }
}

/** Turns an API error into something a student can act on. */
fun friendlyError(status: Int, body: String): String {
    val apiMessage = try {
        ApiJson.decodeFromString(ApiErrorEnvelope.serializer(), body).error?.message
    } catch (e: Exception) {
        null
    }
    return when (status) {
        401 -> "Your API key was not accepted. Check it in Settings."
        403 -> "This API key is not allowed to do that. Check your account on console.anthropic.com."
        404 -> "That model was not found. Pick another model in Settings."
        413 -> "The PDF is too large to send. Choose a smaller file or split it."
        429 -> "Too many requests, or your credit has run out. Wait a minute, then check your plan on console.anthropic.com."
        500, 502, 503, 529 -> "The AI service is busy right now. Please try again in a moment."
        else -> apiMessage ?: "Something went wrong (error $status)."
    }
}
