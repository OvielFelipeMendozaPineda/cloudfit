package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.LoginUseCase
import app.cloudfit.accounts.application.port.output.PasswordHasher
import app.cloudfit.accounts.application.port.output.UserRepository
import app.cloudfit.accounts.domain.Session
import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.ForbiddenException
import app.cloudfit.shared.application.error.UnauthorizedException
import app.cloudfit.shared.application.error.ValidationException

class LoginService(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val sessions: SessionIssuer,
) : LoginUseCase {
    private var timingHash: String? = null

    override suspend fun execute(email: String, password: String): Session {
        val normalized = try {
            AccountValidation.email(email)
        } catch (_: ValidationException) {
            throw invalidCredentials()
        }
        val user = users.findByEmail(normalized)
        val hash = user?.passwordHash
        if (user == null || hash == null) {
            hasher.verify(password, dummyHash())
            throw invalidCredentials()
        }
        if (!hasher.verify(password, hash)) throw invalidCredentials()
        if (!user.emailVerified) throw ForbiddenException("Email not verified", ErrorCodes.EMAIL_NOT_VERIFIED)
        return sessions.start(user)
    }

    private suspend fun dummyHash(): String = timingHash ?: hasher.hash("timing-equalizer-password").also { timingHash = it }

    private fun invalidCredentials() = UnauthorizedException("Invalid email or password", ErrorCodes.INVALID_CREDENTIALS)
}
