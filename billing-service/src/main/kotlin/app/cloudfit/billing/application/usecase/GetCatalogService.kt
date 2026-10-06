package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.GetCatalogUseCase
import app.cloudfit.billing.domain.BillingPolicy
import app.cloudfit.billing.domain.CatalogView
import app.cloudfit.billing.domain.Currency
import app.cloudfit.billing.domain.PackOffer
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.PaymentsMode
import app.cloudfit.billing.domain.PlanOffer

class GetCatalogService(private val policy: BillingPolicy) : GetCatalogUseCase {
    override fun execute(country: String?): CatalogView {
        val currency = currencyFor(country)
        val provider = providerFor(country)
        return CatalogView(
            provider = provider,
            currency = currency,
            packs = policy.catalog.packs.mapNotNull { pack ->
                pack.prices[currency]?.let { PackOffer(pack.code, pack.credits, it, currency) }
            },
            plans = if (currency == Currency.USD) {
                policy.catalog.plans.mapNotNull { plan ->
                    plan.prices[currency]?.let { PlanOffer(plan.code, plan.monthlyCredits, it, currency, plan.features) }
                }
            } else {
                emptyList()
            },
        )
    }

    fun providerFor(country: String?): PaymentProvider = when {
        policy.mode == PaymentsMode.FAKE -> PaymentProvider.FAKE
        isColombia(country) -> PaymentProvider.WOMPI
        else -> PaymentProvider.STRIPE
    }

    fun currencyFor(country: String?): Currency = if (isColombia(country)) Currency.COP else Currency.USD

    private fun isColombia(country: String?) = country?.trim()?.uppercase() == "CO"
}
