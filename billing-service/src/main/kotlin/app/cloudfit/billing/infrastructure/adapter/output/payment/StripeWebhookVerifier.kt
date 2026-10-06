package app.cloudfit.billing.infrastructure.adapter.output.payment

import app.cloudfit.billing.application.port.output.StripeWebhookParser
import app.cloudfit.billing.domain.StripeCheckoutCompleted
import app.cloudfit.billing.domain.StripeEvent
import app.cloudfit.billing.domain.StripeIgnoredEvent
import app.cloudfit.billing.domain.StripeInvoicePaid
import app.cloudfit.billing.domain.StripeSubscriptionChanged
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.application.port.ClockProvider
import java.time.Instant
import kotlin.math.abs
import kotlinx.serialization.json.Json

class StripeWebhookVerifier(
    private val webhookSecret: String,
    private val clock: ClockProvider,
    private val toleranceSeconds: Long = 300,
) : StripeWebhookParser {

    override fun parse(payload: String, signatureHeader: String?): StripeEvent {
        if (webhookSecret.isBlank()) throw ProviderNotConfiguredException("STRIPE_WEBHOOK_SECRET is not set")
        verify(payload, signatureHeader ?: throw ValidationException("Missing Stripe-Signature header"))
        val root = runCatching { Json.parseToJsonElement(payload) }.getOrElse { throw ValidationException("Invalid JSON") }
        val id = root.string("id") ?: throw ValidationException("Stripe event without id")
        val type = root.string("type") ?: throw ValidationException("Stripe event without type")
        val obj = root.at("data", "object")
        return when (type) {
            "checkout.session.completed", "checkout.session.async_payment_succeeded" -> StripeCheckoutCompleted(
                id = id,
                type = type,
                sessionId = obj.string("id") ?: throw ValidationException("Checkout session without id"),
                mode = obj.string("mode") ?: "payment",
                paid = type == "checkout.session.async_payment_succeeded" ||
                    obj.string("payment_status") in setOf("paid", "no_payment_required"),
                paymentId = obj.string("metadata", "paymentId"),
                userId = obj.string("metadata", "userId") ?: obj.string("client_reference_id"),
                productCode = obj.string("metadata", "productCode"),
                customerId = obj.string("customer"),
                subscriptionId = obj.string("subscription"),
            )
            "invoice.paid" -> {
                val metadata = obj.at("subscription_details", "metadata")
                    ?: obj.at("parent", "subscription_details", "metadata")
                val line = obj.at("lines", "data", "0")
                StripeInvoicePaid(
                    id = id,
                    type = type,
                    invoiceId = obj.string("id") ?: throw ValidationException("Invoice without id"),
                    customerId = obj.string("customer"),
                    subscriptionId = obj.string("subscription")
                        ?: obj.string("parent", "subscription_details", "subscription"),
                    planCode = metadata.string("planCode"),
                    userId = metadata.string("userId"),
                    priceId = line.string("price", "id") ?: line.string("pricing", "price_details", "price"),
                    periodEnd = line.long("period", "end")?.let(Instant::ofEpochSecond),
                )
            }
            "customer.subscription.created", "customer.subscription.updated", "customer.subscription.deleted" -> {
                val item = obj.at("items", "data", "0")
                StripeSubscriptionChanged(
                    id = id,
                    type = type,
                    subscriptionId = obj.string("id") ?: throw ValidationException("Subscription without id"),
                    customerId = obj.string("customer"),
                    status = obj.string("status") ?: "incomplete",
                    currentPeriodEnd = (obj.long("current_period_end") ?: item.long("current_period_end"))
                        ?.let(Instant::ofEpochSecond),
                    cancelAtPeriodEnd = obj.bool("cancel_at_period_end") ?: false,
                    planCode = obj.string("metadata", "planCode"),
                    userId = obj.string("metadata", "userId"),
                    priceId = item.string("price", "id"),
                    deleted = type == "customer.subscription.deleted",
                )
            }
            else -> StripeIgnoredEvent(id, type)
        }
    }

    private fun verify(payload: String, header: String) {
        val pairs = header.split(',').mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) null else part.substring(0, idx).trim() to part.substring(idx + 1).trim()
        }
        val timestamp = pairs.firstOrNull { it.first == "t" }?.second?.toLongOrNull()
            ?: throw ValidationException("Invalid Stripe-Signature header")
        val signatures = pairs.filter { it.first == "v1" }.map { it.second }
        val expected = Crypto.hmacSha256Hex(webhookSecret, "$timestamp.$payload")
        if (signatures.none { Crypto.constantTimeEquals(it, expected) }) {
            throw ValidationException("Invalid Stripe signature")
        }
        if (abs(clock.now().epochSecond - timestamp) > toleranceSeconds) {
            throw ValidationException("Stripe signature timestamp outside tolerance")
        }
    }

    companion object {
        fun signatureHeader(secret: String, payload: String, timestamp: Long): String =
            "t=$timestamp,v1=${Crypto.hmacSha256Hex(secret, "$timestamp.$payload")}"
    }
}
