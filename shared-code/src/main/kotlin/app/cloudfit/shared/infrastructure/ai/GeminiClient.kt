package app.cloudfit.shared.infrastructure.ai

import app.cloudfit.shared.application.ai.AiCallContext
import app.cloudfit.shared.application.ai.AiUnavailableException
import app.cloudfit.shared.application.ai.AiUsage
import app.cloudfit.shared.application.ai.AiUsageRecorder
import app.cloudfit.shared.application.ai.GeneratedImage
import app.cloudfit.shared.application.ai.ImageModel
import app.cloudfit.shared.application.ai.Part
import app.cloudfit.shared.application.ai.TextModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.util.Base64
import kotlin.coroutines.coroutineContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class GeminiClient(
    private val config: GeminiConfig,
    private val usageRecorder: AiUsageRecorder,
    private val http: HttpClient = defaultHttpClient(),
) : TextModel, ImageModel {
    private val log = LoggerFactory.getLogger(javaClass)

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
        val response = call(model, request)
        return response.candidates
            .firstOrNull()?.content?.parts.orEmpty()
            .mapNotNull { it.text }
            .joinToString("\n")
            .ifBlank { throw AiUnavailableException("Gemini returned no text") }
    }

    override suspend fun generateImage(model: String, parts: List<Part>): GeneratedImage {
        val request = GenerateContentRequest(
            contents = listOf(Content(role = "user", parts = parts)),
            generationConfig = GenerationConfig(responseModalities = listOf("TEXT", "IMAGE")),
        )
        val response = call(model, request)
        val image = response.candidates
            .firstOrNull()?.content?.parts.orEmpty()
            .firstNotNullOfOrNull { it.inlineData }
            ?: throw AiUnavailableException("Gemini returned no image data")
        return GeneratedImage(
            bytes = Base64.getDecoder().decode(image.data),
            mimeType = image.mimeType,
        )
    }

    private suspend fun call(model: String, request: GenerateContentRequest): GenerateContentResponse {
        if (!config.configured) throw AiUnavailableException("GEMINI_API_KEY is not set")
        val response: HttpResponse = try {
            http.post("${config.baseUrl}/models/$model:generateContent") {
                header("x-goog-api-key", config.apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        } catch (e: Exception) {
            throw AiUnavailableException("Gemini request failed: ${e.message}", e)
        }
        if (!response.status.isSuccess()) {
            throw AiUnavailableException("Gemini ${response.status.value}: ${response.bodyAsText().take(300)}")
        }
        val body: GenerateContentResponse = response.body()
        recordUsage(model, body.usageMetadata)
        return body
    }

    private suspend fun recordUsage(model: String, usage: UsageMetadata?) {
        val context = coroutineContext[AiCallContext] ?: return
        try {
            usageRecorder.record(
                AiUsage(
                    userId = context.userId,
                    operation = context.operation,
                    model = model,
                    inputTokens = usage?.promptTokenCount ?: 0,
                    outputTokens = (usage?.candidatesTokenCount ?: 0) + (usage?.thoughtsTokenCount ?: 0),
                ),
            )
        } catch (e: Exception) {
            log.warn("Could not record AI usage: {}", e.message)
        }
    }

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
        val usageMetadata: UsageMetadata? = null,
    )

    @Serializable
    private data class Candidate(val content: Content? = null)

    @Serializable
    private data class UsageMetadata(
        val promptTokenCount: Int? = null,
        val candidatesTokenCount: Int? = null,
        val thoughtsTokenCount: Int? = null,
        val totalTokenCount: Int? = null,
    )

    companion object {
        fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
            install(HttpTimeout) {
                requestTimeoutMillis = 180_000
                connectTimeoutMillis = 10_000
            }
        }
    }
}
