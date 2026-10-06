package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.GrantCreditsCommand
import app.cloudfit.billing.application.port.input.GrantCreditsUseCase
import app.cloudfit.shared.application.port.TransactionRunner

class GrantCreditsService(
    private val book: CreditBook,
    private val tx: TransactionRunner,
) : GrantCreditsUseCase {
    override suspend fun execute(command: GrantCreditsCommand): Boolean = tx.inTransaction {
        val buckets = book.lock(command.userId)
        book.grant(
            buckets = buckets,
            userId = command.userId,
            bucket = command.bucket,
            amount = command.amount,
            reason = command.reason,
            idempotencyKey = command.idempotencyKey,
            refId = command.refId,
            expiresAt = command.expiresAt,
        )
    }
}
