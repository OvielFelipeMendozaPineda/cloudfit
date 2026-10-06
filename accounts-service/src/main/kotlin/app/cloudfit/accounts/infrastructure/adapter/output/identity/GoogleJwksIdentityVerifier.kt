package app.cloudfit.accounts.infrastructure.adapter.output.identity

import app.cloudfit.accounts.application.port.output.GoogleIdentityVerifier
import app.cloudfit.accounts.domain.AuthProvider
import app.cloudfit.accounts.domain.ExternalIdentity
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import java.net.URI
import java.util.concurrent.TimeUnit

class GoogleJwksIdentityVerifier(
    configuredClientId: String,
    jwkProvider: JwkProvider = JwksProviders.cached(GOOGLE_JWKS_URL),
) : GoogleIdentityVerifier {
    override val clientId: String? = configuredClientId.takeIf { it.isNotBlank() }

    private val validator = clientId?.let {
        JwksTokenValidator(AuthProvider.GOOGLE, it, setOf("accounts.google.com", "https://accounts.google.com"), jwkProvider)
    }

    override suspend fun verify(idToken: String): ExternalIdentity =
        (validator ?: throw ProviderNotConfiguredException("Google sign-in is not configured")).validate(idToken)

    private companion object {
        const val GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs"
    }
}

object JwksProviders {
    fun cached(url: String): JwkProvider = JwkProviderBuilder(URI(url).toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
}
