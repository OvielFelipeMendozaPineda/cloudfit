package app.cloudfit.billing.domain

enum class PaymentsMode { FAKE, LIVE }

data class BillingPolicy(
    val mode: PaymentsMode,
    val catalog: ProductCatalog,
    val adsDailyMax: Int = 3,
    val fakePlanPeriodDays: Long = 30,
    val stripePlanPriceIds: Map<String, String> = emptyMap(),
) {
    fun planCodeForStripePrice(priceId: String?): String? =
        priceId?.let { id -> stripePlanPriceIds.entries.firstOrNull { it.value == id }?.key }
}
