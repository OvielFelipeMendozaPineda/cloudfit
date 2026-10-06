package app.cloudfit.billing.infrastructure.adapter.output.persistence

import app.cloudfit.billing.application.port.output.SubscriptionRepository
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.Subscription
import app.cloudfit.billing.domain.SubscriptionStatus
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class PostgresSubscriptionRepository(private val clock: ClockProvider) : SubscriptionRepository {
    override suspend fun listByUser(userId: UUID): List<Subscription> = dbQuery {
        SubscriptionsTable.selectAll().where { SubscriptionsTable.userId eq userId }.map(::toSubscription)
    }

    override suspend fun findByProviderId(providerSubscriptionId: String): Subscription? = dbQuery {
        SubscriptionsTable.selectAll()
            .where { SubscriptionsTable.providerSubscriptionId eq providerSubscriptionId }
            .map(::toSubscription)
            .singleOrNull()
    }

    override suspend fun save(subscription: Subscription) {
        dbQuery {
            val now = clock.now().toUtc()
            val updated = SubscriptionsTable.update({ SubscriptionsTable.id eq subscription.id }) {
                it[planCode] = subscription.planCode
                it[status] = subscription.status.name
                it[currentPeriodEnd] = subscription.currentPeriodEnd?.toUtc()
                it[cancelAtPeriodEnd] = subscription.cancelAtPeriodEnd
                it[updatedAt] = now
            }
            if (updated == 0) {
                SubscriptionsTable.insert {
                    it[id] = subscription.id
                    it[userId] = subscription.userId
                    it[provider] = subscription.provider.name
                    it[providerSubscriptionId] = subscription.providerSubscriptionId
                    it[planCode] = subscription.planCode
                    it[status] = subscription.status.name
                    it[currentPeriodEnd] = subscription.currentPeriodEnd?.toUtc()
                    it[cancelAtPeriodEnd] = subscription.cancelAtPeriodEnd
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }
    }

    private fun toSubscription(row: ResultRow) = Subscription(
        id = row[SubscriptionsTable.id],
        userId = row[SubscriptionsTable.userId],
        provider = PaymentProvider.valueOf(row[SubscriptionsTable.provider]),
        providerSubscriptionId = row[SubscriptionsTable.providerSubscriptionId],
        planCode = row[SubscriptionsTable.planCode],
        status = SubscriptionStatus.valueOf(row[SubscriptionsTable.status]),
        currentPeriodEnd = row[SubscriptionsTable.currentPeriodEnd]?.toInstant(),
        cancelAtPeriodEnd = row[SubscriptionsTable.cancelAtPeriodEnd],
    )
}
