package app.cloudfit.billing.application.port.input

import app.cloudfit.billing.domain.WalletSummary
import java.util.UUID

interface GetWalletSummaryUseCase {
    suspend fun execute(userId: UUID): WalletSummary
}
