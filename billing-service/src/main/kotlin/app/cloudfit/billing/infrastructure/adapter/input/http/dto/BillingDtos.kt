package app.cloudfit.billing.infrastructure.adapter.input.http.dto

import app.cloudfit.billing.domain.CatalogView
import app.cloudfit.billing.domain.LedgerEntry
import kotlinx.serialization.Serializable

@Serializable
data class PackDto(
    val code: String,
    val credits: Int,
    val price: Long,
    val currency: String,
)

@Serializable
data class PlanDto(
    val code: String,
    val monthlyCredits: Int,
    val price: Long,
    val currency: String,
    val features: List<String>,
)

@Serializable
data class CatalogDto(
    val provider: String,
    val currency: String,
    val packs: List<PackDto>,
    val plans: List<PlanDto>,
) {
    companion object {
        fun from(view: CatalogView) = CatalogDto(
            provider = view.provider.name,
            currency = view.currency.name,
            packs = view.packs.map { PackDto(it.code, it.credits, it.price, it.currency.name) },
            plans = view.plans.map { PlanDto(it.code, it.monthlyCredits, it.price, it.currency.name, it.features) },
        )
    }
}

@Serializable
data class LedgerEntryDto(
    val id: String,
    val delta: Int,
    val bucket: String,
    val reason: String,
    val createdAt: String,
) {
    companion object {
        fun from(entry: LedgerEntry) = LedgerEntryDto(
            id = entry.id.toString(),
            delta = entry.delta,
            bucket = entry.bucket.name,
            reason = entry.reason.name,
            createdAt = entry.createdAt.toString(),
        )
    }
}

@Serializable
data class CheckoutRequestDto(
    val productCode: String,
    val provider: String? = null,
)

@Serializable
data class CheckoutResponseDto(val checkoutUrl: String)

@Serializable
data class PortalResponseDto(val url: String)

@Serializable
data class AdRewardRequestDto(val placement: String = "")

@Serializable
data class AdRewardResponseDto(
    val granted: Boolean,
    val balance: Int,
)

@Serializable
data class WebhookAckDto(val received: Boolean = true)
