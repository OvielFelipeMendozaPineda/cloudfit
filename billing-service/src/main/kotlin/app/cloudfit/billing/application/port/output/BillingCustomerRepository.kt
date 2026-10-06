package app.cloudfit.billing.application.port.output

import java.util.UUID

interface BillingCustomerRepository {
    suspend fun findStripeCustomer(userId: UUID): String?

    suspend fun findUserByStripeCustomer(customerId: String): UUID?

    suspend fun saveStripeCustomer(userId: UUID, customerId: String)
}
