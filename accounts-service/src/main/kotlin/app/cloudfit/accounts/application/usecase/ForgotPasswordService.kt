package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.ForgotPasswordUseCase
import app.cloudfit.accounts.application.port.output.UserRepository
import app.cloudfit.accounts.domain.AccountsPolicy
import app.cloudfit.accounts.domain.EmailTokenPurpose
import app.cloudfit.shared.application.error.ValidationException

class ForgotPasswordService(
    private val users: UserRepository,
    private val tokens: EmailTokenFactory,
    private val mailer: AccountMailer,
    private val policy: AccountsPolicy,
) : ForgotPasswordUseCase {
    override suspend fun execute(email: String) {
        val normalized = try {
            AccountValidation.email(email)
        } catch (_: ValidationException) {
            return
        }
        val user = users.findByEmail(normalized) ?: return
        val token = tokens.issue(user.id, EmailTokenPurpose.RESET, policy.resetTokenTtl)
        mailer.sendPasswordReset(user.email, user.locale, token)
    }
}
