package app.cloudfit.billing.infrastructure.adapter.output.payment

data class StripeConfig(
    val secretKey: String,
    val webhookSecret: String,
    val apiBase: String,
    val appUrl: String,
    val planPriceIds: Map<String, String>,
) {
    val configured: Boolean get() = secretKey.isNotBlank()
}
