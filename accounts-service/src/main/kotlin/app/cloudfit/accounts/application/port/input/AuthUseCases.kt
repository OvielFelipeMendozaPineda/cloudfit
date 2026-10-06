package app.cloudfit.accounts.application.port.input

import app.cloudfit.accounts.domain.AuthProvider
import app.cloudfit.accounts.domain.AuthProviders
import app.cloudfit.accounts.domain.Session

data class RegisterCommand(
    val email: String,
    val password: String,
    val locale: String?,
    val displayName: String?,
)

interface GetAuthProvidersUseCase {
    fun execute(): AuthProviders
}

interface RegisterUseCase {
    suspend fun execute(command: RegisterCommand)
}

interface ResendVerificationUseCase {
    suspend fun execute(email: String)
}

interface VerifyEmailUseCase {
    suspend fun execute(token: String): Session
}

interface LoginUseCase {
    suspend fun execute(email: String, password: String): Session
}

interface SocialLoginUseCase {
    suspend fun execute(provider: AuthProvider, idToken: String, displayName: String?): Session
}

interface RefreshSessionUseCase {
    suspend fun execute(refreshToken: String?): Session
}

interface LogoutUseCase {
    suspend fun execute(refreshToken: String?)
}

interface ForgotPasswordUseCase {
    suspend fun execute(email: String)
}

interface ResetPasswordUseCase {
    suspend fun execute(token: String, password: String)
}
