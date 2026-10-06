package app.cloudfit.billing.infrastructure.adapter.output.payment

data class WompiConfig(
    val publicKey: String,
    val integritySecret: String,
    val eventsSecret: String,
    val environment: String,
    val checkoutUrl: String,
    val appUrl: String,
) {
    val configured: Boolean get() = publicKey.isNotBlank() && integritySecret.isNotBlank()
}
