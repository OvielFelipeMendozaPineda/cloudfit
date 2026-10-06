package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.output.LedgerRepository
import app.cloudfit.billing.application.port.output.WalletRepository
import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.LedgerEntry
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.billing.domain.WalletBucket
import app.cloudfit.shared.application.error.InsufficientCreditsException
import app.cloudfit.shared.application.port.ClockProvider
import java.time.Instant
import java.util.UUID

class CreditBook(
    private val wallet: WalletRepository,
    private val ledger: LedgerRepository,
    private val clock: ClockProvider,
) {
    suspend fun lock(userId: UUID): MutableMap<CreditBucket, WalletBucket> {
        val locked = wallet.lockBuckets(userId).associateBy { it.bucket }
        return CreditBucket.entries.associateWithTo(mutableMapOf()) { locked[it] ?: WalletBucket(it, 0, null) }
    }

    suspend fun grant(
        buckets: MutableMap<CreditBucket, WalletBucket>,
        userId: UUID,
        bucket: CreditBucket,
        amount: Int,
        reason: LedgerReason,
        idempotencyKey: String,
        refId: String? = null,
        expiresAt: Instant? = null,
    ): Boolean {
        require(amount > 0) { "grant amount must be positive" }
        if (!ledger.insertIfAbsent(entry(userId, amount, bucket, reason, idempotencyKey, refId))) return false
        val current = buckets.getValue(bucket)
        apply(buckets, userId, current.copy(balance = current.balance + amount, expiresAt = expiresAt ?: current.expiresAt))
        return true
    }

    suspend fun expire(
        buckets: MutableMap<CreditBucket, WalletBucket>,
        userId: UUID,
        bucket: CreditBucket,
        idempotencyKey: String,
        refId: String? = null,
    ) {
        val current = buckets.getValue(bucket)
        if (current.balance <= 0) return
        if (ledger.insertIfAbsent(entry(userId, -current.balance, bucket, LedgerReason.PLAN_EXPIRE, idempotencyKey, refId))) {
            apply(buckets, userId, current.copy(balance = 0))
        }
    }

    suspend fun debit(
        buckets: MutableMap<CreditBucket, WalletBucket>,
        userId: UUID,
        amount: Int,
        reason: LedgerReason,
        keyPrefix: String,
        refId: String,
    ) {
        val now = clock.now()
        var remaining = amount
        val plan = CreditBucket.entries.mapNotNull { bucket ->
            val take = minOf(buckets.getValue(bucket).available(now), remaining)
            remaining -= take
            if (take > 0) bucket to take else null
        }
        if (remaining > 0) throw InsufficientCreditsException()
        plan.forEach { (bucket, take) ->
            if (ledger.insertIfAbsent(entry(userId, -take, bucket, reason, "$keyPrefix:${bucket.name}", refId))) {
                val current = buckets.getValue(bucket)
                apply(buckets, userId, current.copy(balance = current.balance - take))
            }
        }
    }

    suspend fun restore(
        buckets: MutableMap<CreditBucket, WalletBucket>,
        userId: UUID,
        debits: List<LedgerEntry>,
        reason: LedgerReason,
        keyPrefix: String,
        refId: String,
    ) {
        debits.filter { it.delta < 0 }.forEach { debit ->
            val amount = -debit.delta
            if (ledger.insertIfAbsent(entry(userId, amount, debit.bucket, reason, "$keyPrefix:${debit.bucket.name}", refId))) {
                val current = buckets.getValue(debit.bucket)
                apply(buckets, userId, current.copy(balance = current.balance + amount))
            }
        }
    }

    private suspend fun apply(buckets: MutableMap<CreditBucket, WalletBucket>, userId: UUID, updated: WalletBucket) {
        wallet.setBucket(userId, updated.bucket, updated.balance, updated.expiresAt)
        buckets[updated.bucket] = updated
    }

    private fun entry(
        userId: UUID,
        delta: Int,
        bucket: CreditBucket,
        reason: LedgerReason,
        idempotencyKey: String,
        refId: String?,
    ) = LedgerEntry(
        id = UUID.randomUUID(),
        userId = userId,
        delta = delta,
        bucket = bucket,
        reason = reason,
        refId = refId,
        idempotencyKey = idempotencyKey,
        createdAt = clock.now(),
    )
}
