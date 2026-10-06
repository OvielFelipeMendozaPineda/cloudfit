package app.cloudfit.wiring

import app.cloudfit.billing.application.port.input.GetWalletSummaryUseCase
import app.cloudfit.wardrobe.application.port.output.PlanStatusReader
import java.util.UUID

class PlanStatusAdapter(private val walletSummary: GetWalletSummaryUseCase) : PlanStatusReader {
    override suspend fun hasActivePlan(userId: UUID): Boolean = walletSummary.execute(userId).plan != null
}
