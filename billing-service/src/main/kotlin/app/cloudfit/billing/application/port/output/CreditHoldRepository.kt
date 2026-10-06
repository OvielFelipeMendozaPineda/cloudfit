package app.cloudfit.billing.application.port.output

import app.cloudfit.billing.domain.CreditHold
import app.cloudfit.billing.domain.HoldStatus

interface CreditHoldRepository {
    suspend fun create(hold: CreditHold)

    suspend fun lock(refId: String): CreditHold?

    suspend fun updateStatus(refId: String, status: HoldStatus)
}
