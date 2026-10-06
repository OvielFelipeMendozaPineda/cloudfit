package app.cloudfit.shared.infrastructure.http

import app.cloudfit.shared.domain.AuthenticatedUser
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes

val AUTH_RATE_LIMIT = RateLimitName("auth")
val LOOKS_RATE_LIMIT = RateLimitName("looks")

data class RateLimitSettings(
    val authPerMinute: Int,
    val looksPerMinute: Int,
    val trustProxy: Boolean,
)

fun ApplicationCall.clientIp(trustProxy: Boolean): String {
    if (trustProxy) {
        request.headers["CF-Connecting-IP"]?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        request.headers["X-Forwarded-For"]?.split(',')?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        request.headers["X-Real-IP"]?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    }
    return request.origin.remoteHost
}

fun Application.configureRateLimits(settings: RateLimitSettings) {
    install(RateLimit) {
        register(AUTH_RATE_LIMIT) {
            rateLimiter(limit = settings.authPerMinute, refillPeriod = 1.minutes)
            requestKey { call -> call.clientIp(settings.trustProxy) }
        }
        register(LOOKS_RATE_LIMIT) {
            rateLimiter(limit = settings.looksPerMinute, refillPeriod = 1.minutes)
            requestKey { call -> call.principal<AuthenticatedUser>()?.userId?.toString() ?: call.clientIp(settings.trustProxy) }
        }
    }
}
