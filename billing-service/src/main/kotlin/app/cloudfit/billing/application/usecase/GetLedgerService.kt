package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.GetLedgerUseCase
import app.cloudfit.billing.application.port.output.LedgerRepository
import app.cloudfit.billing.domain.LedgerEntry
import java.util.UUID

class GetLedgerService(private val ledger: LedgerRepository) : GetLedgerUseCase {
    override suspend fun execute(userId: UUID, limit: Int?): List<LedgerEntry> =
        ledger.listByUser(userId, (limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT))

    private companion object {
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 200
    }
}
