package app.cloudfit.shared.infrastructure.auth

import app.cloudfit.shared.domain.AuthenticatedUser
import app.cloudfit.shared.infrastructure.http.ApiErrorResponse
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import java.util.UUID

const val JWT_AUTH = "cloudfit-jwt"

fun AuthenticationConfig.configureJwtAuth(settings: JwtSettings) {
    jwt(JWT_AUTH) {
        verifier(
            JWT.require(Algorithm.HMAC256(settings.secret))
                .withIssuer(settings.issuer)
                .withAudience(settings.audience)
                .withClaim("typ", "access")
                .build(),
        )
        validate { credential ->
            credential.payload.subject
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?.let { AuthenticatedUser(it) }
        }
        challenge { _, _ ->
            call.respond(HttpStatusCode.Unauthorized, ApiErrorResponse("UNAUTHORIZED", "Missing or invalid access token"))
        }
    }
}
