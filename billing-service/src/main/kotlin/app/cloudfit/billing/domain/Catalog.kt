package app.cloudfit.billing.domain

enum class PaymentProvider { STRIPE, WOMPI, FAKE }

enum class Currency { USD, COP }

enum class ProductKind { PACK, PLAN }

data class PackProduct(
    val code: String,
    val credits: Int,
    val prices: Map<Currency, Long>,
)

data class PlanProduct(
    val code: String,
    val monthlyCredits: Int,
    val prices: Map<Currency, Long>,
    val features: List<String>,
)

data class ProductCatalog(
    val packs: List<PackProduct>,
    val plans: List<PlanProduct>,
) {
    fun pack(code: String): PackProduct? = packs.firstOrNull { it.code == code }

    fun plan(code: String): PlanProduct? = plans.firstOrNull { it.code == code }
}

data class PackOffer(
    val code: String,
    val credits: Int,
    val price: Long,
    val currency: Currency,
)

data class PlanOffer(
    val code: String,
    val monthlyCredits: Int,
    val price: Long,
    val currency: Currency,
    val features: List<String>,
)

data class CatalogView(
    val provider: PaymentProvider,
    val currency: Currency,
    val packs: List<PackOffer>,
    val plans: List<PlanOffer>,
)
