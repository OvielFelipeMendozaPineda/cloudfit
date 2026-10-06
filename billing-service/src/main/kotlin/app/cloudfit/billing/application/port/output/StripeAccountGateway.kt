package app.cloudfit.billing.application.port.output

import java.util.UUID

interface StripeAccountGateway {
    val configured: Boolean

    suspend fun createCustomer(userId: UUID, email: String?): String

    suspend fun createPortalSession(customerId: String): String

    suspend fun cancelSubscription(subscriptionId: String)
}
