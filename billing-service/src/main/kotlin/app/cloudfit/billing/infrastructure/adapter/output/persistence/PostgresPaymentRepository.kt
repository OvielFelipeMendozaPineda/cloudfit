package app.cloudfit.billing.infrastructure.adapter.output.persistence

import app.cloudfit.billing.application.port.output.PaymentRepository
import app.cloudfit.billing.domain.Currency
import app.cloudfit.billing.domain.Payment
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.PaymentStatus
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class PostgresPaymentRepository(private val clock: ClockProvider) : PaymentRepository {
    override suspend fun create(payment: Payment) {
        dbQuery {
            val now = clock.now().toUtc()
            PaymentsTable.insert {
                it[id] = payment.id
                it[userId] = payment.userId
                it[provider] = payment.provider.name
                it[productCode] = payment.productCode
                it[amount] = payment.amount
                it[currency] = payment.currency.name
                it[status] = payment.status.name
                it[providerRef] = payment.providerRef
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    override suspend fun findById(id: UUID): Payment? = dbQuery {
        PaymentsTable.selectAll().where { PaymentsTable.id eq id }.mapNotNull(::toPayment).singleOrNull()
    }

    override suspend fun findByProviderRef(providerRef: String): Payment? = dbQuery {
        PaymentsTable.selectAll().where { PaymentsTable.providerRef eq providerRef }.mapNotNull(::toPayment).singleOrNull()
    }

    override suspend fun update(id: UUID, status: PaymentStatus, providerRef: String?) {
        dbQuery {
            PaymentsTable.update({ PaymentsTable.id eq id }) {
                it[PaymentsTable.status] = status.name
                if (providerRef != null) it[PaymentsTable.providerRef] = providerRef
                it[updatedAt] = clock.now().toUtc()
            }
        }
    }

    private fun toPayment(row: ResultRow): Payment? {
        val userId = row[PaymentsTable.userId] ?: return null
        return Payment(
            id = row[PaymentsTable.id],
            userId = userId,
            provider = PaymentProvider.valueOf(row[PaymentsTable.provider]),
            productCode = row[PaymentsTable.productCode],
            amount = row[PaymentsTable.amount],
            currency = Currency.valueOf(row[PaymentsTable.currency]),
            status = PaymentStatus.valueOf(row[PaymentsTable.status]),
            providerRef = row[PaymentsTable.providerRef],
        )
    }
}
