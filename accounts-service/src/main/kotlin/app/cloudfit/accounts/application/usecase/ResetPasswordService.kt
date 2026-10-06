package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.ResetPasswordUseCase
import app.cloudfit.accounts.application.port.output.EmailTokenRepository
import app.cloudfit.accounts.application.port.output.PasswordHasher
import app.cloudfit.accounts.application.port.output.RefreshTokenRepository
import app.cloudfit.accounts.application.port.output.UserRepository
import app.cloudfit.accounts.application.port.output.WelcomeBonusGranter
import app.cloudfit.accounts.domain.EmailTokenPurpose
import app.cloudfit.accounts.domain.OpaqueTokens
import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.UnauthorizedException
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.TransactionRunner

class ResetPasswordService(
    private val emailTokens: EmailTokenRepository,
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val hasher: PasswordHasher,
    private val welcomeBonus: WelcomeBonusGranter,
    private val tx: TransactionRunner,
    private val clock: ClockProvider,
) : ResetPasswordUseCase {
    override suspend fun execute(token: String, password: String) {
        val passwordHash = hasher.hash(AccountValidation.password(password))
        tx.inTransaction {
            val now = clock.now()
            val resetToken = emailTokens.lockValid(OpaqueTokens.hash(token.trim()), EmailTokenPurpose.RESET, now)
                ?: throw UnauthorizedException("Invalid or expired reset token", ErrorCodes.INVALID_TOKEN)
            emailTokens.markUsed(resetToken.id, now)
            val user = users.findById(resetToken.userId)
                ?: throw UnauthorizedException("Invalid or expired reset token", ErrorCodes.INVALID_TOKEN)
            users.update(user.copy(passwordHash = passwordHash, emailVerifiedAt = user.emailVerifiedAt ?: now))
            emailTokens.invalidateAll(user.id, EmailTokenPurpose.RESET, now)
            refreshTokens.revokeAllForUser(user.id, now)
            if (!user.emailVerified) welcomeBonus.grant(user.id)
        }
    }
}
