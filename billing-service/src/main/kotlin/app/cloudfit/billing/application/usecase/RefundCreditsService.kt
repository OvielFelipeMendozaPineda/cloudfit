package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.RefundCreditsUseCase
import app.cloudfit.billing.application.port.output.CreditHoldRepository
import app.cloudfit.billing.application.port.output.LedgerRepository
import app.cloudfit.billing.domain.HoldStatus
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.shared.application.port.TransactionRunner

class RefundCreditsService(
    private val book: CreditBook,
    private val holds: CreditHoldRepository,
    private val ledger: LedgerRepository,
    private val tx: TransactionRunner,
) : RefundCreditsUseCase {
    override suspend fun execute(refId: String): Boolean = tx.inTransaction {
        val hold = holds.lock(refId)
        if (hold == null || hold.status != HoldStatus.RESERVED) return@inTransaction false
        val buckets = book.lock(hold.userId)
        val debits = ledger.findByRef(refId, LedgerReason.LOOK_RESERVED)
        book.restore(buckets, hold.userId, debits, LedgerReason.LOOK_REFUND, "hold:$refId:refund", refId)
        holds.updateStatus(refId, HoldStatus.REFUNDED)
        true
    }
}
