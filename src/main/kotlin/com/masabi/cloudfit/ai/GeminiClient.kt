package com.masabi.cloudfit.ai

import com.masabi.cloudfit.config.GeminiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * [TextModel] + [ImageModel] backed by the Gemini REST API (Google Generative Language).
 *
 * Gemini is the single AI provider for cloud-fit: text reasoning (outfit assembly), vision
 * (garment tagging) and image generation all go through here.
 */
class GeminiClient(
    private val config: GeminiConfig,
) : TextModel, ImageModel {

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    override suspend fun generate(
        model: String,
        systemPrompt: String,
        parts: List<Part>,
        asJson: Boolean,
    ): String {
        val request = GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            contents = listOf(Content(role = "user", parts = parts)),
            generationConfig = GenerationConfig(
                responseMimeType = if (asJson) "application/json" else null,
            ),
        )
        val response: GenerateContentResponse =
            http.post("${config.baseUrl}/models/$model:generateContent") {
                header("x-goog-api-key", config.apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

        return response.candidates
            .firstOrNull()?.content?.parts.orEmpty()
            .mapNotNull { it.text }
            .joinToString("\n")
    }

    override suspend fun generateImage(model: String, parts: List<Part>): GeneratedImage {
        val request = GenerateContentRequest(
            contents = listOf(Content(role = "user", parts = parts)),
            generationConfig = GenerationConfig(responseModalities = listOf("TEXT", "IMAGE")),
        )
        val response: GenerateContentResponse =
            http.post("${config.baseUrl}/models/$model:generateContent") {
                header("x-goog-api-key", config.apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

        val image = response.candidates
            .firstOrNull()?.content?.parts.orEmpty()
            .firstNotNullOfOrNull { it.inlineData }
            ?: error("Gemini returned no image data")
        return GeneratedImage(
            bytes = Base64.getDecoder().decode(image.data),
            mimeType = image.mimeType,
        )
    }

    // ---- Wire format ----

    @Serializable
    private data class Content(
        val role: String? = null,
        val parts: List<Part>,
    )

    @Serializable
    private data class GenerationConfig(
        val responseMimeType: String? = null,
        val temperature: Double? = null,
        val responseModalities: List<String>? = null,
    )

    @Serializable
    private data class GenerateContentRequest(
        val systemInstruction: Content? = null,
        val contents: List<Content>,
        val generationConfig: GenerationConfig? = null,
    )

    @Serializable
    private data class GenerateContentResponse(
        val candidates: List<Candidate> = emptyList(),
    )

    @Serializable
    private data class Candidate(val content: Content? = null)
}
