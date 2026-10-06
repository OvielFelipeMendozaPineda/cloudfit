package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.ClaimAdRewardUseCase
import app.cloudfit.billing.application.port.input.GetWalletSummaryUseCase
import app.cloudfit.billing.application.port.output.LedgerRepository
import app.cloudfit.billing.domain.BillingPolicy
import app.cloudfit.billing.domain.CreditBalance
import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.shared.application.error.ForbiddenException
import app.cloudfit.shared.application.error.RateLimitedException
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.TransactionRunner
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

class ClaimAdRewardService(
    private val book: CreditBook,
    private val ledger: LedgerRepository,
    private val walletSummary: GetWalletSummaryUseCase,
    private val policy: BillingPolicy,
    private val tx: TransactionRunner,
    private val clock: ClockProvider,
) : ClaimAdRewardUseCase {
    override suspend fun execute(userId: UUID, placement: String): Int {
        if (placement.isBlank() || placement.length > 64) throw ValidationException("placement is required (max 64 chars)")
        if (walletSummary.execute(userId).plan != null) throw ForbiddenException("Ad rewards are not available with an active plan")
        return tx.inTransaction {
            val now = clock.now()
            val buckets = book.lock(userId)
            val startOfDay = now.atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).toInstant()
            if (ledger.countByReasonSince(userId, LedgerReason.AD_REWARD, startOfDay) >= policy.adsDailyMax) {
                throw RateLimitedException("Daily ad reward limit reached")
            }
            book.grant(
                buckets = buckets,
                userId = userId,
                bucket = CreditBucket.REWARD,
                amount = 1,
                reason = LedgerReason.AD_REWARD,
                idempotencyKey = "ad:$userId:${UUID.randomUUID()}",
                refId = placement.trim(),
            )
            CreditBalance.of(buckets.values, now).total
        }
    }
}
