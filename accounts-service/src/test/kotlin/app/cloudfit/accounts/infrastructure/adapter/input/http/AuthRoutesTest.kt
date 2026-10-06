package app.cloudfit.accounts.infrastructure.adapter.input.http

import app.cloudfit.accounts.support.AccountsTestKit
import app.cloudfit.shared.infrastructure.http.RateLimitSettings
import app.cloudfit.shared.infrastructure.http.configureApiStatusPages
import app.cloudfit.shared.infrastructure.http.configureRateLimits
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

class AuthRoutesTest : StringSpec({

    fun ApplicationTestBuilder.setup(kit: AccountsTestKit, authPerMinute: Int = 100) {
        application {
            install(ContentNegotiation) { json() }
            configureApiStatusPages()
            configureRateLimits(RateLimitSettings(authPerMinute, 10, trustProxy = true))
            routing { route("/api/v1") { registerAuthRoutes(kit.authUseCases, RefreshCookieSettings(secure = true)) } }
        }
    }

    suspend fun ApplicationTestBuilder.registeredSessionCookie(kit: AccountsTestKit): String {
        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"ana@example.com","password":"supersecret1","locale":"en"}""")
        }.status shouldBe HttpStatusCode.Accepted
        val verify = client.post("/api/v1/auth/verify-email") {
            contentType(ContentType.Application.Json)
            setBody("""{"token":"${kit.emails.lastToken()}"}""")
        }
        verify.status shouldBe HttpStatusCode.OK
        val setCookie = verify.headers[HttpHeaders.SetCookie]!!
        setCookie shouldContain "cf_refresh="
        setCookie shouldContain "HttpOnly"
        setCookie shouldContain "Secure"
        setCookie shouldContain "SameSite=Lax"
        setCookie shouldContain "Path=/api/v1/auth"
        return setCookie.substringBefore(';')
    }

    "refresh requires the X-CloudFit-Client header and rotates the cookie" {
        val kit = AccountsTestKit()
        testApplication {
            setup(kit)
            val cookie = registeredSessionCookie(kit)

            client.post("/api/v1/auth/refresh") { header(HttpHeaders.Cookie, cookie) }.status shouldBe HttpStatusCode.Forbidden

            val refreshed = client.post("/api/v1/auth/refresh") {
                header(HttpHeaders.Cookie, cookie)
                header("X-CloudFit-Client", "web")
            }
            refreshed.status shouldBe HttpStatusCode.OK
            refreshed.bodyAsText() shouldContain "\"expiresIn\":900"

            val reused = client.post("/api/v1/auth/refresh") {
                header(HttpHeaders.Cookie, cookie)
                header("X-CloudFit-Client", "web")
            }
            reused.status shouldBe HttpStatusCode.Unauthorized
            reused.bodyAsText() shouldContain "INVALID_TOKEN"
            reused.headers[HttpHeaders.SetCookie]!! shouldContain "Max-Age=0"
        }
    }

    "logout answers 204 and clears the cookie" {
        val kit = AccountsTestKit()
        testApplication {
            setup(kit)
            val cookie = registeredSessionCookie(kit)
            val response = client.post("/api/v1/auth/logout") {
                header(HttpHeaders.Cookie, cookie)
                header("X-CloudFit-Client", "web")
            }
            response.status shouldBe HttpStatusCode.NoContent
            response.headers[HttpHeaders.SetCookie]!! shouldContain "Max-Age=0"
        }
    }

    "auth routes are rate limited per IP" {
        val kit = AccountsTestKit()
        testApplication {
            setup(kit, authPerMinute = 2)
            repeat(2) {
                client.post("/api/v1/auth/forgot-password") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"x@y.co"}""")
                }.status shouldBe HttpStatusCode.Accepted
            }
            val limited = client.post("/api/v1/auth/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"x@y.co"}""")
            }
            limited.status shouldBe HttpStatusCode.TooManyRequests
            limited.bodyAsText() shouldContain "RATE_LIMITED"
        }
    }

    "malformed bodies answer VALIDATION_ERROR" {
        val kit = AccountsTestKit()
        testApplication {
            setup(kit)
            val response = client.post("/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("{not json")
            }
            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText() shouldContain "VALIDATION_ERROR"
        }
    }
})
