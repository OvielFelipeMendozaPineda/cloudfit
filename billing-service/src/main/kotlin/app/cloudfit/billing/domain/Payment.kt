package app.cloudfit.billing.domain

import java.util.UUID

enum class PaymentStatus { PENDING, PAID, FAILED }

data class Payment(
    val id: UUID,
    val userId: UUID,
    val provider: PaymentProvider,
    val productCode: String,
    val amount: Long,
    val currency: Currency,
    val status: PaymentStatus,
    val providerRef: String?,
)
