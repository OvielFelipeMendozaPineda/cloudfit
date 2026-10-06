package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.VerifyEmailUseCase
import app.cloudfit.accounts.application.port.output.EmailTokenRepository
import app.cloudfit.accounts.application.port.output.UserRepository
import app.cloudfit.accounts.application.port.output.WelcomeBonusGranter
import app.cloudfit.accounts.domain.EmailTokenPurpose
import app.cloudfit.accounts.domain.OpaqueTokens
import app.cloudfit.accounts.domain.Session
import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.UnauthorizedException
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.TransactionRunner

class VerifyEmailService(
    private val emailTokens: EmailTokenRepository,
    private val users: UserRepository,
    private val welcomeBonus: WelcomeBonusGranter,
    private val sessions: SessionIssuer,
    private val tx: TransactionRunner,
    private val clock: ClockProvider,
) : VerifyEmailUseCase {
    override suspend fun execute(token: String): Session = tx.inTransaction {
        val now = clock.now()
        val emailToken = emailTokens.lockValid(OpaqueTokens.hash(token.trim()), EmailTokenPurpose.VERIFY, now)
            ?: throw UnauthorizedException("Invalid or expired verification token", ErrorCodes.INVALID_TOKEN)
        emailTokens.markUsed(emailToken.id, now)
        val user = users.findById(emailToken.userId)
            ?: throw UnauthorizedException("Invalid or expired verification token", ErrorCodes.INVALID_TOKEN)
        val verified = user.copy(
            emailVerifiedAt = user.emailVerifiedAt ?: now,
            passwordHash = emailToken.passwordHash ?: user.passwordHash,
        )
        users.update(verified)
        emailTokens.invalidateAll(user.id, EmailTokenPurpose.VERIFY, now)
        welcomeBonus.grant(user.id)
        sessions.start(verified)
    }
}
