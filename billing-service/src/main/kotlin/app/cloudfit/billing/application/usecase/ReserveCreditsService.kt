package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.ReserveCreditsUseCase
import app.cloudfit.billing.application.port.output.CreditHoldRepository
import app.cloudfit.billing.domain.CreditHold
import app.cloudfit.billing.domain.HoldStatus
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.shared.application.port.TransactionRunner
import java.util.UUID

class ReserveCreditsService(
    private val book: CreditBook,
    private val holds: CreditHoldRepository,
    private val tx: TransactionRunner,
) : ReserveCreditsUseCase {
    override suspend fun execute(userId: UUID, refId: String, amount: Int) {
        require(amount > 0) { "reserve amount must be positive" }
        tx.inTransaction {
            val buckets = book.lock(userId)
            book.debit(buckets, userId, amount, LedgerReason.LOOK_RESERVED, "hold:$refId:reserve", refId)
            holds.create(CreditHold(refId = refId, userId = userId, amount = amount, status = HoldStatus.RESERVED))
        }
    }
}
