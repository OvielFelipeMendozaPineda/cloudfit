package app.cloudfit.billing.application.port.output

import app.cloudfit.billing.domain.Payment
import app.cloudfit.billing.domain.PaymentStatus
import java.util.UUID

interface PaymentRepository {
    suspend fun create(payment: Payment)

    suspend fun findById(id: UUID): Payment?

    suspend fun findByProviderRef(providerRef: String): Payment?

    suspend fun update(id: UUID, status: PaymentStatus, providerRef: String?)
}
