package app.cloudfit.shared.infrastructure.http

import app.cloudfit.shared.application.error.ConflictException
import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.ForbiddenException
import app.cloudfit.shared.application.error.InsufficientCreditsException
import app.cloudfit.shared.application.error.NotFoundException
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.RateLimitedException
import app.cloudfit.shared.application.error.UnauthorizedException
import app.cloudfit.shared.application.error.UnprocessableException
import app.cloudfit.shared.application.error.UpstreamException
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.infrastructure.auth.JWT_AUTH
import app.cloudfit.shared.infrastructure.auth.JwtSettings
import app.cloudfit.shared.infrastructure.auth.configureJwtAuth
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.time.Instant
import java.util.UUID

class StatusPagesTest : StringSpec({
    val cases = mapOf(
        "validation" to (ValidationException("bad") to (400 to "VALIDATION_ERROR")),
        "unauthorized" to (UnauthorizedException("no", ErrorCodes.INVALID_CREDENTIALS) to (401 to "INVALID_CREDENTIALS")),
        "credits" to (InsufficientCreditsException() to (402 to "INSUFFICIENT_CREDITS")),
        "forbidden" to (ForbiddenException("no", ErrorCodes.WARDROBE_LIMIT_REACHED) to (403 to "WARDROBE_LIMIT_REACHED")),
        "missing" to (NotFoundException() to (404 to "NOT_FOUND")),
        "conflict" to (ConflictException("dup") to (409 to "CONFLICT")),
        "incomplete" to (UnprocessableException("x", ErrorCodes.WARDROBE_INCOMPLETE) to (422 to "WARDROBE_INCOMPLETE")),
        "limited" to (RateLimitedException() to (429 to "RATE_LIMITED")),
        "unconfigured" to (ProviderNotConfiguredException("x") to (501 to "PROVIDER_NOT_CONFIGURED")),
        "upstream" to (UpstreamException("x", ErrorCodes.AI_UNAVAILABLE) to (502 to "AI_UNAVAILABLE")),
        "crash" to (IllegalStateException("secret detail") to (500 to "INTERNAL_ERROR")),
    )
    val settings = JwtSettings(secret = "s".repeat(32), keyId = "v1", issuer = "cloudfit", audience = "cloudfit-api")

    "every application error maps to the contract status and {error, message} body" {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureApiStatusPages()
                routing { cases.forEach { (path, case) -> get("/$path") { throw case.first } } }
            }
            cases.forEach { (path, case) ->
                val response = client.get("/$path")
                response.status.value shouldBe case.second.first
                val body = response.bodyAsText()
                body.contains("\"error\":\"${case.second.second}\"") shouldBe true
                body.contains("secret detail") shouldBe false
            }
            client.get("/does-not-exist").bodyAsText().contains("NOT_FOUND") shouldBe true
        }
    }

    "jwt auth accepts access tokens and rejects anything else with UNAUTHORIZED" {
        val userId = UUID.randomUUID()
        fun token(typ: String = "access", secret: String = settings.secret) = JWT.create()
            .withIssuer(settings.issuer)
            .withAudience(settings.audience)
            .withSubject(userId.toString())
            .withClaim("typ", typ)
            .withExpiresAt(Instant.now().plusSeconds(60))
            .sign(Algorithm.HMAC256(secret))

        testApplication {
            application {
                install(ContentNegotiation) { json() }
                configureApiStatusPages()
                install(Authentication) { configureJwtAuth(settings) }
                routing {
                    authenticate(JWT_AUTH) { get("/private") { call.respondText(call.requireAuthenticatedUser().userId.toString()) } }
                }
            }
            client.get("/private") { header(HttpHeaders.Authorization, "Bearer ${token()}") }.bodyAsText() shouldBe userId.toString()
            listOf(token(typ = "refresh"), token(secret = "o".repeat(32)), "garbage").forEach {
                val response = client.get("/private") { header(HttpHeaders.Authorization, "Bearer $it") }
                response.status shouldBe HttpStatusCode.Unauthorized
                response.bodyAsText().contains("UNAUTHORIZED") shouldBe true
            }
            client.get("/private").status shouldBe HttpStatusCode.Unauthorized
        }
    }
})
