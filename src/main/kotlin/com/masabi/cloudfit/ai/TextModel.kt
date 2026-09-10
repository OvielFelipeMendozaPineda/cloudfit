package com.masabi.cloudfit.ai

import kotlinx.serialization.Serializable

/** A prompt part: text, or an inline image for vision / image-to-image. */
@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null,
)

/** Base64 image payload. */
@Serializable
data class InlineData(
    val mimeType: String,
    val data: String,
)

/**
 * Abstraction over a generative text model, so callers (e.g. the stylist) depend on this rather
 * than on a concrete provider. [GeminiClient] is the production implementation; tests can supply
 * a fake.
 */
interface TextModel {
    suspend fun generate(
        model: String,
        systemPrompt: String,
        parts: List<Part>,
        asJson: Boolean = false,
    ): String
}
