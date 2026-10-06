package app.cloudfit.shared.infrastructure.ai

import app.cloudfit.shared.application.ai.AiCallContext
import app.cloudfit.shared.application.ai.AiUnavailableException
import app.cloudfit.shared.application.ai.AiUsage
import app.cloudfit.shared.application.ai.Part
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.util.UUID
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class GeminiClientTest : StringSpec({
    val config = GeminiConfig("test-key", "https://gemini.test/v1beta", "stylist", "tagger", "image")

    fun client(status: HttpStatusCode, body: String) = HttpClient(MockEngine { request ->
        request.headers["x-goog-api-key"] shouldBe "test-key"
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

    "returns the text and records token usage for the calling user" {
        val usages = mutableListOf<AiUsage>()
        val body = """{"candidates":[{"content":{"parts":[{"text":"{\"ok\":true}"}]}}],
            "usageMetadata":{"promptTokenCount":120,"candidatesTokenCount":30,"thoughtsTokenCount":5,"totalTokenCount":155}}"""
        val gemini = GeminiClient(config, { usages += it }, client(HttpStatusCode.OK, body))
        val user = UUID.randomUUID()

        val text = withContext(AiCallContext(user, "PICK")) { gemini.generate("stylist", "sys", listOf(Part(text = "hi")), asJson = true) }

        text shouldBe "{\"ok\":true}"
        usages.single() shouldBe AiUsage(user, "PICK", "stylist", 120, 35)
    }

    "does not record usage outside an AI call context" {
        val usages = mutableListOf<AiUsage>()
        val body = """{"candidates":[{"content":{"parts":[{"text":"x"}]}}]}"""
        GeminiClient(config, { usages += it }, client(HttpStatusCode.OK, body)).generate("m", "s", emptyList())
        usages shouldBe emptyList()
    }

    "maps HTTP errors and empty answers to AiUnavailableException" {
        shouldThrow<AiUnavailableException> {
            GeminiClient(config, {}, client(HttpStatusCode.ServiceUnavailable, "{}")).generate("m", "s", emptyList())
        }
        shouldThrow<AiUnavailableException> {
            GeminiClient(config, {}, client(HttpStatusCode.OK, """{"candidates":[]}""")).generateImage("image", emptyList())
        }
        shouldThrow<AiUnavailableException> {
            GeminiClient(config.copy(apiKey = ""), {}, client(HttpStatusCode.OK, "{}")).generate("m", "s", emptyList())
        }
    }

    "decodes inline image data" {
        val body = """{"candidates":[{"content":{"parts":[{"inlineData":{"mimeType":"image/png","data":"UE5H"}}]}}]}"""
        val image = GeminiClient(config, {}, client(HttpStatusCode.OK, body)).generateImage("image", emptyList())
        image.mimeType shouldBe "image/png"
        String(image.bytes) shouldBe "PNG"
    }
})
