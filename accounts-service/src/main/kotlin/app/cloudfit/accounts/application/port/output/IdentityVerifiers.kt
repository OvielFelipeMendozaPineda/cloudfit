package app.cloudfit.accounts.application.port.output

import app.cloudfit.accounts.domain.ExternalIdentity

interface IdentityTokenVerifier {
    val clientId: String?

    suspend fun verify(idToken: String): ExternalIdentity
}

interface GoogleIdentityVerifier : IdentityTokenVerifier

interface AppleIdentityVerifier : IdentityTokenVerifier
