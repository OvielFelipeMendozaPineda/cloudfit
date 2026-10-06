package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.GetWalletSummaryUseCase
import app.cloudfit.billing.application.port.output.SubscriptionRepository
import app.cloudfit.billing.application.port.output.WalletRepository
import app.cloudfit.billing.domain.ActivePlan
import app.cloudfit.billing.domain.CreditBalance
import app.cloudfit.billing.domain.WalletSummary
import app.cloudfit.shared.application.port.ClockProvider
import java.util.UUID

class GetWalletSummaryService(
    private val wallet: WalletRepository,
    private val subscriptions: SubscriptionRepository,
    private val clock: ClockProvider,
) : GetWalletSummaryUseCase {
    override suspend fun execute(userId: UUID): WalletSummary {
        val now = clock.now()
        val plan = subscriptions.listByUser(userId)
            .filter { it.isActive(now) }
            .maxByOrNull { it.currentPeriodEnd ?: now }
            ?.let { ActivePlan(code = it.planCode, status = it.status, renewsAt = it.currentPeriodEnd) }
        return WalletSummary(
            balance = CreditBalance.of(wallet.findBuckets(userId), now),
            plan = plan,
        )
    }
}
