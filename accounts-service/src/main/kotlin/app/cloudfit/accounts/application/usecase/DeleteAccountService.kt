package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.DeleteAccountUseCase
import app.cloudfit.accounts.application.port.output.AccountDataEraser
import app.cloudfit.accounts.application.port.output.UserRepository
import java.util.UUID
import org.slf4j.LoggerFactory

class DeleteAccountService(
    private val users: UserRepository,
    private val erasers: List<AccountDataEraser>,
) : DeleteAccountUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(userId: UUID) {
        erasers.forEach { eraser ->
            runCatching { eraser.erase(userId) }
                .onFailure { log.error("Account data eraser failed for {}: {}", userId, it.message) }
        }
        users.delete(userId)
    }
}
