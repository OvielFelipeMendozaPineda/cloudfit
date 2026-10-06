package app.cloudfit.billing.application.port.output

import app.cloudfit.billing.domain.Subscription
import java.util.UUID

interface SubscriptionRepository {
    suspend fun listByUser(userId: UUID): List<Subscription>

    suspend fun findByProviderId(providerSubscriptionId: String): Subscription?

    suspend fun save(subscription: Subscription)
}
