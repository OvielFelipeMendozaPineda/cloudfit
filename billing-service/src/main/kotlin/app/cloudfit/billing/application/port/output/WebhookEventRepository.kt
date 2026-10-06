package app.cloudfit.billing.application.port.output

import app.cloudfit.billing.domain.PaymentProvider

fun interface WebhookEventRepository {
    suspend fun registerIfNew(provider: PaymentProvider, eventId: String, type: String): Boolean
}
