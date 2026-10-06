package app.cloudfit.billing.application.port.input

import app.cloudfit.billing.domain.LedgerEntry
import java.util.UUID

interface GetLedgerUseCase {
    suspend fun execute(userId: UUID, limit: Int?): List<LedgerEntry>
}
