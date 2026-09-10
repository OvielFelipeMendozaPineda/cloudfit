package com.masabi.cloudfit.ai

/** A generated image: raw bytes plus the MIME type the model returned (e.g. image/jpeg). */
data class GeneratedImage(
    val bytes: ByteArray,
    val mimeType: String,
)

/**
 * Abstraction over a generative image model (Gemini image). Callers depend on this rather than on
 * the concrete client, so the renderer is testable with a fake and the provider is swappable.
 */
interface ImageModel {
    /** Generates an image from the given prompt parts (text and/or reference images). */
    suspend fun generateImage(model: String, parts: List<Part>): GeneratedImage
}
