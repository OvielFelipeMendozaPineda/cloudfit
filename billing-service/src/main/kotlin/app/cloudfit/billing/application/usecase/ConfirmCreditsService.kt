package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.ConfirmCreditsUseCase
import app.cloudfit.billing.application.port.output.CreditHoldRepository
import app.cloudfit.billing.domain.HoldStatus
import app.cloudfit.shared.application.port.TransactionRunner

class ConfirmCreditsService(
    private val holds: CreditHoldRepository,
    private val tx: TransactionRunner,
) : ConfirmCreditsUseCase {
    override suspend fun execute(refId: String) {
        tx.inTransaction {
            val hold = holds.lock(refId)
            if (hold?.status == HoldStatus.RESERVED) holds.updateStatus(refId, HoldStatus.CONFIRMED)
        }
    }
}
