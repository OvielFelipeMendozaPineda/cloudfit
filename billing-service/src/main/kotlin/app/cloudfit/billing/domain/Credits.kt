package app.cloudfit.billing.domain

import java.time.Instant
import java.util.UUID

enum class CreditBucket { PLAN, FREE, REWARD, PACK }

enum class LedgerReason { SIGNUP_BONUS, PURCHASE, PLAN_GRANT, PLAN_EXPIRE, LOOK_RESERVED, LOOK_REFUND, AD_REWARD }

data class WalletBucket(
    val bucket: CreditBucket,
    val balance: Int,
    val expiresAt: Instant?,
) {
    fun available(now: Instant): Int = if (expiresAt != null && !expiresAt.isAfter(now)) 0 else balance
}

data class LedgerEntry(
    val id: UUID,
    val userId: UUID,
    val delta: Int,
    val bucket: CreditBucket,
    val reason: LedgerReason,
    val refId: String?,
    val idempotencyKey: String,
    val createdAt: Instant,
)

data class CreditBalance(
    val plan: Int,
    val free: Int,
    val pack: Int,
    val reward: Int,
) {
    val total: Int get() = plan + free + pack + reward

    companion object {
        fun of(buckets: Collection<WalletBucket>, now: Instant): CreditBalance {
            fun amount(bucket: CreditBucket) = buckets.firstOrNull { it.bucket == bucket }?.available(now) ?: 0
            return CreditBalance(
                plan = amount(CreditBucket.PLAN),
                free = amount(CreditBucket.FREE),
                pack = amount(CreditBucket.PACK),
                reward = amount(CreditBucket.REWARD),
            )
        }
    }
}

enum class HoldStatus { RESERVED, CONFIRMED, REFUNDED }

data class CreditHold(
    val refId: String,
    val userId: UUID,
    val amount: Int,
    val status: HoldStatus,
)
