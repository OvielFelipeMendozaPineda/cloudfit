package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.billing.domain.PlanProduct
import java.time.Instant
import java.util.UUID

class PlanGranter(private val book: CreditBook) {
    suspend fun grantPeriod(userId: UUID, plan: PlanProduct, periodEnd: Instant?, idempotencyKey: String, refId: String?) {
        val buckets = book.lock(userId)
        book.expire(buckets, userId, CreditBucket.PLAN, "$idempotencyKey:expire", refId)
        book.grant(
            buckets = buckets,
            userId = userId,
            bucket = CreditBucket.PLAN,
            amount = plan.monthlyCredits,
            reason = LedgerReason.PLAN_GRANT,
            idempotencyKey = idempotencyKey,
            refId = refId,
            expiresAt = periodEnd,
        )
    }

    suspend fun expireAll(userId: UUID, idempotencyKey: String, refId: String?) {
        val buckets = book.lock(userId)
        book.expire(buckets, userId, CreditBucket.PLAN, idempotencyKey, refId)
    }
}
