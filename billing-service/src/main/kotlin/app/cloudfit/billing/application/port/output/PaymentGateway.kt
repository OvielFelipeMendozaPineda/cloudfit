package app.cloudfit.billing.application.port.output

import app.cloudfit.billing.domain.Currency
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.ProductKind
import java.util.UUID

data class CheckoutRequest(
    val paymentId: UUID,
    val userId: UUID,
    val kind: ProductKind,
    val productCode: String,
    val description: String,
    val amount: Long,
    val currency: Currency,
    val customerId: String?,
    val customerEmail: String?,
)

data class CheckoutSession(
    val url: String,
    val providerRef: String?,
)

interface PaymentGateway {
    val provider: PaymentProvider
    val configured: Boolean

    suspend fun createCheckout(request: CheckoutRequest): CheckoutSession
}
