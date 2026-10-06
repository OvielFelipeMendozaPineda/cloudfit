package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.GetAuthProvidersUseCase
import app.cloudfit.accounts.application.port.output.AppleIdentityVerifier
import app.cloudfit.accounts.application.port.output.GoogleIdentityVerifier
import app.cloudfit.accounts.domain.AccountsPolicy
import app.cloudfit.accounts.domain.AuthProviders

class GetAuthProvidersService(
    private val google: GoogleIdentityVerifier,
    private val apple: AppleIdentityVerifier,
    private val policy: AccountsPolicy,
) : GetAuthProvidersUseCase {
    override fun execute(): AuthProviders = AuthProviders(
        googleClientId = google.clientId,
        appleClientId = apple.clientId,
        appleRedirectUri = apple.clientId?.let { policy.appleRedirectUri ?: "${policy.appUrl}/auth/apple/callback" },
    )
}
