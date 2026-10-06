package app.cloudfit.accounts.infrastructure.adapter.output.identity

import app.cloudfit.accounts.domain.AuthProvider
import app.cloudfit.accounts.domain.ExternalIdentity
import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.UnauthorizedException
import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.security.interfaces.RSAPublicKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JwksTokenValidator(
    private val provider: AuthProvider,
    private val clientId: String,
    private val issuers: Set<String>,
    private val jwkProvider: JwkProvider,
) {
    suspend fun validate(idToken: String): ExternalIdentity {
        val decoded = runCatching { JWT.decode(idToken) }.getOrElse { throw invalid("Malformed identity token") }
        val keyId = decoded.keyId ?: throw invalid("Identity token without kid")
        val publicKey = runCatching { withContext(Dispatchers.IO) { jwkProvider.get(keyId).publicKey as RSAPublicKey } }
            .getOrElse { throw invalid("Unknown signing key") }
        val verified = runCatching {
            JWT.require(Algorithm.RSA256(publicKey, null))
                .withIssuer(*issuers.toTypedArray())
                .withAudience(clientId)
                .acceptLeeway(LEEWAY_SECONDS)
                .build()
                .verify(decoded)
        }.getOrElse { throw invalid("Identity token rejected: ${it.message}") }
        return ExternalIdentity(
            provider = provider,
            subject = verified.subject ?: throw invalid("Identity token without subject"),
            email = verified.getClaim("email").asString(),
            emailVerified = emailVerified(verified),
            displayName = verified.getClaim("name").asString(),
            locale = verified.getClaim("locale").asString(),
        )
    }

    private fun emailVerified(token: DecodedJWT): Boolean {
        val claim = token.getClaim("email_verified")
        return claim.asBoolean() ?: claim.asString()?.toBooleanStrictOrNull() ?: false
    }

    private fun invalid(message: String) = UnauthorizedException(message, ErrorCodes.INVALID_TOKEN)

    private companion object {
        const val LEEWAY_SECONDS = 60L
    }
}
