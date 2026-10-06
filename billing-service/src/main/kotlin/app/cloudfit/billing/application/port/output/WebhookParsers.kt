package app.cloudfit.billing.application.port.output

import app.cloudfit.billing.domain.StripeEvent
import app.cloudfit.billing.domain.WompiTransactionEvent

fun interface StripeWebhookParser {
    fun parse(payload: String, signatureHeader: String?): StripeEvent
}

fun interface WompiWebhookParser {
    fun parse(payload: String, checksumHeader: String?): WompiTransactionEvent?
}
