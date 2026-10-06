package app.cloudfit.billing.domain

import java.time.Instant

sealed interface StripeEvent {
    val id: String
    val type: String
}

data class StripeCheckoutCompleted(
    override val id: String,
    override val type: String,
    val sessionId: String,
    val mode: String,
    val paid: Boolean,
    val paymentId: String?,
    val userId: String?,
    val productCode: String?,
    val customerId: String?,
    val subscriptionId: String?,
) : StripeEvent

data class StripeInvoicePaid(
    override val id: String,
    override val type: String,
    val invoiceId: String,
    val customerId: String?,
    val subscriptionId: String?,
    val planCode: String?,
    val userId: String?,
    val priceId: String?,
    val periodEnd: Instant?,
) : StripeEvent

data class StripeSubscriptionChanged(
    override val id: String,
    override val type: String,
    val subscriptionId: String,
    val customerId: String?,
    val status: String,
    val currentPeriodEnd: Instant?,
    val cancelAtPeriodEnd: Boolean,
    val planCode: String?,
    val userId: String?,
    val priceId: String?,
    val deleted: Boolean,
) : StripeEvent

data class StripeIgnoredEvent(
    override val id: String,
    override val type: String,
) : StripeEvent

data class WompiTransactionEvent(
    val transactionId: String,
    val reference: String,
    val status: String,
    val amountInCents: Long,
    val currency: String,
)
