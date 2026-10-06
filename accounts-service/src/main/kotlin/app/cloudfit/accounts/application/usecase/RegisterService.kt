package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.RegisterCommand
import app.cloudfit.accounts.application.port.input.RegisterUseCase
import app.cloudfit.accounts.application.port.output.PasswordHasher
import app.cloudfit.accounts.application.port.output.UserRepository
import app.cloudfit.accounts.domain.AccountsPolicy
import app.cloudfit.accounts.domain.EmailTokenPurpose
import app.cloudfit.accounts.domain.User
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.TransactionRunner
import java.util.UUID

class RegisterService(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val tokens: EmailTokenFactory,
    private val mailer: AccountMailer,
    private val policy: AccountsPolicy,
    private val tx: TransactionRunner,
    private val clock: ClockProvider,
) : RegisterUseCase {
    override suspend fun execute(command: RegisterCommand) {
        val email = AccountValidation.email(command.email)
        val password = AccountValidation.password(command.password)
        val locale = AccountValidation.locale(command.locale)
        val displayName = AccountValidation.displayName(command.displayName)
        val passwordHash = hasher.hash(password)

        val notification: suspend () -> Unit = tx.inTransaction {
            val existing = users.findByEmail(email)
            when {
                existing == null -> {
                    val user = User(
                        id = UUID.randomUUID(),
                        email = email,
                        passwordHash = passwordHash,
                        emailVerifiedAt = null,
                        displayName = displayName,
                        locale = locale,
                        createdAt = clock.now(),
                    )
                    users.create(user)
                    val token = tokens.issue(user.id, EmailTokenPurpose.VERIFY, policy.verifyTokenTtl, passwordHash)
                    suspend { mailer.sendVerification(email, locale, token) }
                }
                existing.emailVerified -> suspend { mailer.sendAlreadyRegistered(email, existing.locale) }
                else -> {
                    users.update(existing.copy(passwordHash = passwordHash, locale = locale, displayName = displayName ?: existing.displayName))
                    val token = tokens.issue(existing.id, EmailTokenPurpose.VERIFY, policy.verifyTokenTtl, passwordHash)
                    suspend { mailer.sendVerification(email, locale, token) }
                }
            }
        }
        notification()
    }
}
