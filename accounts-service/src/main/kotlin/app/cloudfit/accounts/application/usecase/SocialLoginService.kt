package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.SocialLoginUseCase
import app.cloudfit.accounts.application.port.output.AppleIdentityVerifier
import app.cloudfit.accounts.application.port.output.GoogleIdentityVerifier
import app.cloudfit.accounts.application.port.output.IdentityRepository
import app.cloudfit.accounts.application.port.output.UserRepository
import app.cloudfit.accounts.application.port.output.WelcomeBonusGranter
import app.cloudfit.accounts.domain.AuthProvider
import app.cloudfit.accounts.domain.ExternalIdentity
import app.cloudfit.accounts.domain.Session
import app.cloudfit.accounts.domain.User
import app.cloudfit.shared.application.error.ConflictException
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.TransactionRunner
import app.cloudfit.shared.domain.AppLocale
import java.util.UUID

class SocialLoginService(
    private val google: GoogleIdentityVerifier,
    private val apple: AppleIdentityVerifier,
    private val users: UserRepository,
    private val identities: IdentityRepository,
    private val welcomeBonus: WelcomeBonusGranter,
    private val sessions: SessionIssuer,
    private val tx: TransactionRunner,
    private val clock: ClockProvider,
) : SocialLoginUseCase {
    override suspend fun execute(provider: AuthProvider, idToken: String, displayName: String?): Session {
        val verifier = when (provider) {
            AuthProvider.GOOGLE -> google
            AuthProvider.APPLE -> apple
            AuthProvider.PASSWORD -> throw ValidationException("Unsupported provider")
        }
        if (verifier.clientId == null) throw ProviderNotConfiguredException("$provider sign-in is not configured")
        if (idToken.isBlank()) throw ValidationException("idToken is required")
        val identity = verifier.verify(idToken)
        val fallbackName = AccountValidation.displayName(displayName)

        return tx.inTransaction {
            val linked = identities.findUserId(provider, identity.subject)?.let { users.findById(it) }
            val (user, newlyVerified) = linked?.let { it to false } ?: resolveUnlinked(identity, fallbackName)
            if (newlyVerified) welcomeBonus.grant(user.id)
            sessions.start(user)
        }
    }

    private suspend fun resolveUnlinked(identity: ExternalIdentity, fallbackName: String?): Pair<User, Boolean> {
        val email = identity.email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            ?: throw ValidationException("Identity token has no email")
        val now = clock.now()
        val existing = users.findByEmail(email)
        if (existing != null) {
            if (!identity.emailVerified) throw ConflictException("An account with this email already exists")
            val linked = if (existing.emailVerified) existing else existing.copy(emailVerifiedAt = now, passwordHash = null)
            if (linked != existing) users.update(linked)
            identities.link(linked.id, identity)
            return linked to !existing.emailVerified
        }
        val created = User(
            id = UUID.randomUUID(),
            email = email,
            passwordHash = null,
            emailVerifiedAt = if (identity.emailVerified) now else null,
            displayName = AccountValidation.displayName(identity.displayName) ?: fallbackName,
            locale = AppLocale.parse(identity.locale?.substringBefore('-')) ?: AppLocale.EN,
            createdAt = now,
        )
        users.create(created)
        identities.link(created.id, identity)
        return created to created.emailVerified
    }
}
