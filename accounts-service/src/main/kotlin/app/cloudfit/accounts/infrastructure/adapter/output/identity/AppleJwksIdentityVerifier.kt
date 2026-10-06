package app.cloudfit.accounts.infrastructure.adapter.output.identity

import app.cloudfit.accounts.application.port.output.AppleIdentityVerifier
import app.cloudfit.accounts.domain.AuthProvider
import app.cloudfit.accounts.domain.ExternalIdentity
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import com.auth0.jwk.JwkProvider

class AppleJwksIdentityVerifier(
    configuredClientId: String,
    jwkProvider: JwkProvider = JwksProviders.cached(APPLE_JWKS_URL),
) : AppleIdentityVerifier {
    override val clientId: String? = configuredClientId.takeIf { it.isNotBlank() }

    private val validator = clientId?.let {
        JwksTokenValidator(AuthProvider.APPLE, it, setOf("https://appleid.apple.com"), jwkProvider)
    }

    override suspend fun verify(idToken: String): ExternalIdentity =
        (validator ?: throw ProviderNotConfiguredException("Apple sign-in is not configured")).validate(idToken)

    private companion object {
        const val APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys"
    }
}
