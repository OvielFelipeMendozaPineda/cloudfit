package app.cloudfit.billing.application.port.input

import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.LedgerReason
import java.time.Instant
import java.util.UUID

data class GrantCreditsCommand(
    val userId: UUID,
    val bucket: CreditBucket,
    val amount: Int,
    val reason: LedgerReason,
    val idempotencyKey: String,
    val refId: String? = null,
    val expiresAt: Instant? = null,
)

interface GrantCreditsUseCase {
    suspend fun execute(command: GrantCreditsCommand): Boolean
}

interface ReserveCreditsUseCase {
    suspend fun execute(userId: UUID, refId: String, amount: Int = 1)
}

interface ConfirmCreditsUseCase {
    suspend fun execute(refId: String)
}

interface RefundCreditsUseCase {
    suspend fun execute(refId: String): Boolean
}
