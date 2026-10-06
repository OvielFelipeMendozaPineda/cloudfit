package app.cloudfit.shared.infrastructure.auth

import io.ktor.server.config.ApplicationConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class JwtSettings(
    val secret: String,
    val keyId: String,
    val issuer: String,
    val audience: String,
    val accessTokenTtl: Duration = 15.minutes,
) {
    init {
        check(secret.toByteArray().size >= MIN_SECRET_BYTES) {
            "JWT_SECRET is missing or shorter than $MIN_SECRET_BYTES bytes — refusing to start."
        }
    }

    companion object {
        const val MIN_SECRET_BYTES = 32

        fun from(config: ApplicationConfig): JwtSettings {
            val section = config.config("cloudfit.auth")
            return JwtSettings(
                secret = section.propertyOrNull("jwtSecret")?.getString().orEmpty(),
                keyId = section.propertyOrNull("jwtKeyId")?.getString()?.ifBlank { null } ?: "v1",
                issuer = section.propertyOrNull("jwtIssuer")?.getString()?.ifBlank { null } ?: "cloudfit",
                audience = section.propertyOrNull("jwtAudience")?.getString()?.ifBlank { null } ?: "cloudfit-api",
            )
        }
    }
}
