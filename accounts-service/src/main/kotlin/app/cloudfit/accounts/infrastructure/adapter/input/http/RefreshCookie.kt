package app.cloudfit.accounts.infrastructure.adapter.input.http

import app.cloudfit.accounts.domain.Session
import io.ktor.http.Cookie
import io.ktor.server.application.ApplicationCall
import io.ktor.util.date.GMTDate
import java.time.Duration

data class RefreshCookieSettings(
    val secure: Boolean,
    val name: String = "cf_refresh",
    val path: String = "/api/v1/auth",
)

fun ApplicationCall.readRefreshCookie(settings: RefreshCookieSettings): String? = request.cookies[settings.name]

fun ApplicationCall.writeRefreshCookie(settings: RefreshCookieSettings, session: Session) {
    val maxAge = Duration.between(java.time.Instant.now(), session.refreshExpiresAt).seconds.coerceAtLeast(0)
    response.cookies.append(
        Cookie(
            name = settings.name,
            value = session.refreshToken,
            maxAge = maxAge.toInt(),
            path = settings.path,
            secure = settings.secure,
            httpOnly = true,
            extensions = mapOf("SameSite" to "Lax"),
        ),
    )
}

fun ApplicationCall.clearRefreshCookie(settings: RefreshCookieSettings) {
    response.cookies.append(
        Cookie(
            name = settings.name,
            value = "",
            maxAge = 0,
            expires = GMTDate(0),
            path = settings.path,
            secure = settings.secure,
            httpOnly = true,
            extensions = mapOf("SameSite" to "Lax"),
        ),
    )
}
