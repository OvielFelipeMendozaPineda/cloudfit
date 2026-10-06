package app.cloudfit.billing.application.port.input

interface HandleStripeWebhookUseCase {
    suspend fun execute(payload: String, signatureHeader: String?)
}

interface HandleWompiWebhookUseCase {
    suspend fun execute(payload: String, checksumHeader: String?)
}
