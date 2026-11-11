package com.studyflow.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The JSON shapes of Anthropic's Messages API.
 * Fields with defaults are left out when they equal the default (encodeDefaults = false), so a
 * text block never carries an empty "source" and a document block never carries an empty "text".
 */
@Serializable
data class ApiRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<ApiMessage>,
)

@Serializable
data class ApiMessage(val role: String, val content: List<ApiBlock>)

@Serializable
data class ApiBlock(
    val type: String,
    val text: String? = null,
    val source: ApiSource? = null,
    @SerialName("cache_control") val cacheControl: ApiCacheControl? = null,
)

@Serializable
data class ApiSource(
    val type: String,
    @SerialName("media_type") val mediaType: String,
    val data: String,
)

@Serializable
data class ApiCacheControl(val type: String)

@Serializable
data class ApiResponse(
    val content: List<ApiBlock> = emptyList(),
    @SerialName("stop_reason") val stopReason: String? = null,
)

@Serializable
data class ApiErrorEnvelope(val error: ApiErrorBody? = null)

@Serializable
data class ApiErrorBody(val type: String? = null, val message: String? = null)

val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}
