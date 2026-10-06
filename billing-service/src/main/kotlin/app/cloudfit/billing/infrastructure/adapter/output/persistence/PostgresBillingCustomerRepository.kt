package app.cloudfit.billing.infrastructure.adapter.output.persistence

import app.cloudfit.billing.application.port.output.BillingCustomerRepository
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import java.util.UUID
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll

class PostgresBillingCustomerRepository(private val clock: ClockProvider) : BillingCustomerRepository {
    override suspend fun findStripeCustomer(userId: UUID): String? = dbQuery {
        BillingCustomersTable.selectAll()
            .where { BillingCustomersTable.userId eq userId }
            .map { it[BillingCustomersTable.stripeCustomerId] }
            .singleOrNull()
    }

    override suspend fun findUserByStripeCustomer(customerId: String): UUID? = dbQuery {
        BillingCustomersTable.selectAll()
            .where { BillingCustomersTable.stripeCustomerId eq customerId }
            .map { it[BillingCustomersTable.userId] }
            .singleOrNull()
    }

    override suspend fun saveStripeCustomer(userId: UUID, customerId: String) {
        dbQuery {
            BillingCustomersTable.insertIgnore {
                it[BillingCustomersTable.userId] = userId
                it[stripeCustomerId] = customerId
                it[createdAt] = clock.now().toUtc()
            }
        }
    }
}
