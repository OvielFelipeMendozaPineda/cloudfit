package app.cloudfit.accounts.infrastructure.adapter.output.security

import app.cloudfit.accounts.application.port.output.TokenIssuer
import app.cloudfit.accounts.domain.AccessToken
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.infrastructure.auth.JwtSettings
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.UUID
import kotlin.time.toJavaDuration

class JwtTokenIssuer(
    private val settings: JwtSettings,
    private val clock: ClockProvider,
) : TokenIssuer {
    private val algorithm = Algorithm.HMAC256(settings.secret)

    override fun issueAccessToken(userId: UUID): AccessToken {
        val now = clock.now()
        val ttl = settings.accessTokenTtl.toJavaDuration()
        val token = JWT.create()
            .withKeyId(settings.keyId)
            .withIssuer(settings.issuer)
            .withAudience(settings.audience)
            .withSubject(userId.toString())
            .withClaim("typ", "access")
            .withJWTId(UUID.randomUUID().toString())
            .withIssuedAt(now)
            .withExpiresAt(now.plus(ttl))
            .sign(algorithm)
        return AccessToken(token = token, expiresInSeconds = ttl.seconds)
    }
}
