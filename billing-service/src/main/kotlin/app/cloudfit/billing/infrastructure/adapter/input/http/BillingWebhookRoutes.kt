package app.cloudfit.billing.infrastructure.adapter.input.http

import app.cloudfit.billing.application.port.input.HandleStripeWebhookUseCase
import app.cloudfit.billing.application.port.input.HandleWompiWebhookUseCase
import app.cloudfit.billing.infrastructure.adapter.input.http.dto.WebhookAckDto
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.registerBillingWebhookRoutes(
    handleStripeWebhook: HandleStripeWebhookUseCase,
    handleWompiWebhook: HandleWompiWebhookUseCase,
) {
    post("/billing/webhooks/stripe") {
        handleStripeWebhook.execute(call.receiveText(), call.request.headers["Stripe-Signature"])
        call.respond(WebhookAckDto())
    }
    post("/billing/webhooks/wompi") {
        handleWompiWebhook.execute(call.receiveText(), call.request.headers["X-Event-Checksum"])
        call.respond(WebhookAckDto())
    }
}
