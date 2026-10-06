package app.cloudfit.billing.infrastructure.adapter.output.payment

import app.cloudfit.billing.application.port.output.CheckoutRequest
import app.cloudfit.billing.application.port.output.CheckoutSession
import app.cloudfit.billing.application.port.output.PaymentGateway
import app.cloudfit.billing.application.port.output.StripeAccountGateway
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.ProductKind
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.UpstreamException
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.isSuccess
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class StripePaymentGateway(
    private val config: StripeConfig,
    private val http: HttpClient,
) : PaymentGateway, StripeAccountGateway {
    override val provider = PaymentProvider.STRIPE
    override val configured: Boolean get() = config.configured

    override suspend fun createCheckout(request: CheckoutRequest): CheckoutSession {
        val isPlan = request.kind == ProductKind.PLAN
        val form = Parameters.build {
            append("mode", if (isPlan) "subscription" else "payment")
            append("success_url", "${config.appUrl}/billing/success?session_id={CHECKOUT_SESSION_ID}")
            append("cancel_url", "${config.appUrl}/billing/cancel")
            append("client_reference_id", request.userId.toString())
            request.customerId?.let { append("customer", it) }
            metadata("metadata", request)
            append("line_items[0][quantity]", "1")
            val priceId = if (isPlan) config.planPriceIds[request.productCode]?.takeIf { it.isNotBlank() } else null
            if (priceId != null) {
                append("line_items[0][price]", priceId)
            } else {
                append("line_items[0][price_data][currency]", request.currency.name.lowercase())
                append("line_items[0][price_data][unit_amount]", request.amount.toString())
                append("line_items[0][price_data][product_data][name]", request.description)
                if (isPlan) append("line_items[0][price_data][recurring][interval]", "month")
            }
            if (isPlan) metadata("subscription_data[metadata]", request) else metadata("payment_intent_data[metadata]", request)
        }
        val body = post("/v1/checkout/sessions", form)
        return CheckoutSession(
            url = body.string("url") ?: throw UpstreamException("Stripe checkout session has no url"),
            providerRef = body.string("id"),
        )
    }

    override suspend fun createCustomer(userId: UUID, email: String?): String {
        val body = post(
            "/v1/customers",
            Parameters.build {
                email?.let { append("email", it) }
                append("metadata[userId]", userId.toString())
            },
        )
        return body.string("id") ?: throw UpstreamException("Stripe customer has no id")
    }

    override suspend fun createPortalSession(customerId: String): String {
        val body = post(
            "/v1/billing_portal/sessions",
            Parameters.build {
                append("customer", customerId)
                append("return_url", "${config.appUrl}/billing")
            },
        )
        return body.string("url") ?: throw UpstreamException("Stripe portal session has no url")
    }

    override suspend fun cancelSubscription(subscriptionId: String) {
        requireConfigured()
        val response = http.delete("${config.apiBase}/v1/subscriptions/$subscriptionId") { bearerAuth(config.secretKey) }
        ensureSuccess(response)
    }

    private fun ParametersBuilder.metadata(prefix: String, request: CheckoutRequest) {
        append("$prefix[userId]", request.userId.toString())
        append("$prefix[paymentId]", request.paymentId.toString())
        append("$prefix[productCode]", request.productCode)
        if (request.kind == ProductKind.PLAN) append("$prefix[planCode]", request.productCode)
    }

    private suspend fun post(path: String, form: Parameters): JsonElement {
        requireConfigured()
        val response = try {
            http.submitForm(url = "${config.apiBase}$path", formParameters = form) { bearerAuth(config.secretKey) }
        } catch (e: Exception) {
            throw UpstreamException("Stripe request failed: ${e.message}")
        }
        ensureSuccess(response)
        return Json.parseToJsonElement(response.bodyAsText())
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            throw UpstreamException("Stripe ${response.status.value}: ${response.bodyAsText().take(300)}")
        }
    }

    private fun requireConfigured() {
        if (!configured) throw ProviderNotConfiguredException("STRIPE is not configured")
    }
}
