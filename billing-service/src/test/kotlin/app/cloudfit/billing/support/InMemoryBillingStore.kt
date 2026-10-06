package app.cloudfit.billing.support

import app.cloudfit.billing.application.port.output.BillingCustomerRepository
import app.cloudfit.billing.application.port.output.CreditHoldRepository
import app.cloudfit.billing.application.port.output.LedgerRepository
import app.cloudfit.billing.application.port.output.PaymentRepository
import app.cloudfit.billing.application.port.output.SubscriptionRepository
import app.cloudfit.billing.application.port.output.WalletRepository
import app.cloudfit.billing.application.port.output.WebhookEventRepository
import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.CreditHold
import app.cloudfit.billing.domain.HoldStatus
import app.cloudfit.billing.domain.LedgerEntry
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.billing.domain.Payment
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.PaymentStatus
import app.cloudfit.billing.domain.Subscription
import app.cloudfit.billing.domain.WalletBucket
import java.time.Instant
import java.util.UUID

class InMemoryWalletRepository : WalletRepository {
    val buckets = mutableMapOf<Pair<UUID, CreditBucket>, WalletBucket>()

    override suspend fun lockBuckets(userId: UUID): List<WalletBucket> = CreditBucket.entries.map { bucket ->
        buckets.getOrPut(userId to bucket) { WalletBucket(bucket, 0, null) }
    }

    override suspend fun findBuckets(userId: UUID): List<WalletBucket> =
        buckets.filterKeys { it.first == userId }.values.toList()

    override suspend fun setBucket(userId: UUID, bucket: CreditBucket, balance: Int, expiresAt: Instant?) {
        check(balance >= 0) { "negative balance" }
        buckets[userId to bucket] = WalletBucket(bucket, balance, expiresAt)
    }

    fun balance(userId: UUID, bucket: CreditBucket): Int = buckets[userId to bucket]?.balance ?: 0
}

class InMemoryLedgerRepository : LedgerRepository {
    val entries = mutableListOf<LedgerEntry>()

    override suspend fun insertIfAbsent(entry: LedgerEntry): Boolean {
        if (entries.any { it.idempotencyKey == entry.idempotencyKey }) return false
        entries += entry
        return true
    }

    override suspend fun findByRef(refId: String, reason: LedgerReason): List<LedgerEntry> =
        entries.filter { it.refId == refId && it.reason == reason }

    override suspend fun listByUser(userId: UUID, limit: Int): List<LedgerEntry> =
        entries.filter { it.userId == userId }.sortedByDescending { it.createdAt }.take(limit)

    override suspend fun countByReasonSince(userId: UUID, reason: LedgerReason, since: Instant): Int =
        entries.count { it.userId == userId && it.reason == reason && !it.createdAt.isBefore(since) }
}

class InMemoryCreditHoldRepository : CreditHoldRepository {
    val holds = mutableMapOf<String, CreditHold>()

    override suspend fun create(hold: CreditHold) {
        check(hold.refId !in holds) { "duplicate hold" }
        holds[hold.refId] = hold
    }

    override suspend fun lock(refId: String): CreditHold? = holds[refId]

    override suspend fun updateStatus(refId: String, status: HoldStatus) {
        holds.computeIfPresent(refId) { _, hold -> hold.copy(status = status) }
    }
}

class InMemorySubscriptionRepository : SubscriptionRepository {
    val subscriptions = mutableMapOf<UUID, Subscription>()

    override suspend fun listByUser(userId: UUID): List<Subscription> = subscriptions.values.filter { it.userId == userId }

    override suspend fun findByProviderId(providerSubscriptionId: String): Subscription? =
        subscriptions.values.firstOrNull { it.providerSubscriptionId == providerSubscriptionId }

    override suspend fun save(subscription: Subscription) {
        subscriptions[subscription.id] = subscription
    }
}

class InMemoryPaymentRepository : PaymentRepository {
    val payments = mutableMapOf<UUID, Payment>()

    override suspend fun create(payment: Payment) {
        payments[payment.id] = payment
    }

    override suspend fun findById(id: UUID): Payment? = payments[id]

    override suspend fun findByProviderRef(providerRef: String): Payment? =
        payments.values.firstOrNull { it.providerRef == providerRef }

    override suspend fun update(id: UUID, status: PaymentStatus, providerRef: String?) {
        payments.computeIfPresent(id) { _, p -> p.copy(status = status, providerRef = providerRef ?: p.providerRef) }
    }
}

class InMemoryBillingCustomerRepository : BillingCustomerRepository {
    val customers = mutableMapOf<UUID, String>()

    override suspend fun findStripeCustomer(userId: UUID): String? = customers[userId]

    override suspend fun findUserByStripeCustomer(customerId: String): UUID? =
        customers.entries.firstOrNull { it.value == customerId }?.key

    override suspend fun saveStripeCustomer(userId: UUID, customerId: String) {
        customers.putIfAbsent(userId, customerId)
    }
}

class InMemoryWebhookEventRepository : WebhookEventRepository {
    val events = mutableSetOf<Pair<PaymentProvider, String>>()

    override suspend fun registerIfNew(provider: PaymentProvider, eventId: String, type: String): Boolean =
        events.add(provider to eventId)
}
