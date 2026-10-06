package app.cloudfit.billing.infrastructure.config

import app.cloudfit.billing.domain.BillingPolicy
import app.cloudfit.billing.domain.Currency
import app.cloudfit.billing.domain.PackProduct
import app.cloudfit.billing.domain.PaymentsMode
import app.cloudfit.billing.domain.PlanProduct
import app.cloudfit.billing.domain.ProductCatalog
import app.cloudfit.billing.infrastructure.adapter.output.payment.StripeConfig
import app.cloudfit.billing.infrastructure.adapter.output.payment.WompiConfig
import io.ktor.server.config.ApplicationConfig

data class BillingConfig(
    val policy: BillingPolicy,
    val stripe: StripeConfig,
    val wompi: WompiConfig,
) {
    companion object {
        fun from(config: ApplicationConfig, appUrl: String, devMode: Boolean): BillingConfig {
            val section = config.config("cloudfit.billing")
            val mode = section.property("mode").getString().trim().uppercase()
            val stripe = section.config("stripe")
            val wompi = section.config("wompi")
            val planPriceIds = mapOf(
                "PLUS" to stripe.propertyOrNull("pricePlus")?.getString().orEmpty(),
                "PRO" to stripe.propertyOrNull("pricePro")?.getString().orEmpty(),
            ).filterValues { it.isNotBlank() }
            return BillingConfig(
                policy = BillingPolicy(
                    mode = when {
                        mode.isEmpty() -> if (devMode) PaymentsMode.FAKE else PaymentsMode.LIVE
                        else -> PaymentsMode.entries.firstOrNull { it.name == mode }
                            ?: error("PAYMENTS_MODE must be 'fake' or 'live', got '$mode'")
                    },
                    catalog = ProductCatalog(
                        packs = section.configList("packs").map { pack ->
                            PackProduct(
                                code = pack.property("code").getString(),
                                credits = pack.property("credits").getString().toInt(),
                                prices = prices(pack),
                            )
                        },
                        plans = section.configList("plans").map { plan ->
                            PlanProduct(
                                code = plan.property("code").getString(),
                                monthlyCredits = plan.property("monthlyCredits").getString().toInt(),
                                prices = prices(plan),
                                features = plan.propertyOrNull("features")?.getList().orEmpty(),
                            )
                        },
                    ),
                    adsDailyMax = section.propertyOrNull("adsDailyMax")?.getString()?.toIntOrNull() ?: 3,
                    stripePlanPriceIds = planPriceIds,
                ),
                stripe = StripeConfig(
                    secretKey = stripe.property("secretKey").getString(),
                    webhookSecret = stripe.property("webhookSecret").getString(),
                    apiBase = stripe.propertyOrNull("apiBase")?.getString() ?: "https://api.stripe.com",
                    appUrl = appUrl,
                    planPriceIds = planPriceIds,
                ),
                wompi = WompiConfig(
                    publicKey = wompi.property("publicKey").getString(),
                    integritySecret = wompi.property("integritySecret").getString(),
                    eventsSecret = wompi.property("eventsSecret").getString(),
                    environment = wompi.propertyOrNull("environment")?.getString() ?: "sandbox",
                    checkoutUrl = wompi.propertyOrNull("checkoutUrl")?.getString() ?: "https://checkout.wompi.co/p/",
                    appUrl = appUrl,
                ),
            )
        }

        private fun prices(product: ApplicationConfig): Map<Currency, Long> = Currency.entries.mapNotNull { currency ->
            product.propertyOrNull(currency.name.lowercase())?.getString()?.toLongOrNull()?.let { currency to it }
        }.toMap()
    }
}
