package app.cloudfit.billing.infrastructure.adapter.output.payment

import app.cloudfit.billing.application.port.output.CheckoutRequest
import app.cloudfit.billing.application.port.output.CheckoutSession
import app.cloudfit.billing.application.port.output.PaymentGateway
import app.cloudfit.billing.domain.Currency
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.ProductKind
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.ValidationException
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom

class WompiPaymentGateway(private val config: WompiConfig) : PaymentGateway {
    override val provider = PaymentProvider.WOMPI
    override val configured: Boolean get() = config.configured

    override suspend fun createCheckout(request: CheckoutRequest): CheckoutSession {
        if (!configured) throw ProviderNotConfiguredException("WOMPI is not configured")
        if (request.kind != ProductKind.PACK || request.currency != Currency.COP) {
            throw ValidationException("Wompi only sells credit packs in COP")
        }
        val reference = request.paymentId.toString()
        val url = URLBuilder().takeFrom(config.checkoutUrl).apply {
            parameters.append("public-key", config.publicKey)
            parameters.append("currency", request.currency.name)
            parameters.append("amount-in-cents", request.amount.toString())
            parameters.append("reference", reference)
            parameters.append("signature:integrity", integritySignature(reference, request.amount, request.currency.name))
            parameters.append("redirect-url", "${config.appUrl}/billing/success")
            request.customerEmail?.let { parameters.append("customer-data:email", it) }
        }.buildString()
        return CheckoutSession(url = url, providerRef = null)
    }

    fun integritySignature(reference: String, amountInCents: Long, currency: String): String =
        Crypto.sha256Hex("$reference$amountInCents$currency${config.integritySecret}")
}
