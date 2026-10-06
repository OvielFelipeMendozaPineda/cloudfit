package app.cloudfit.billing.support

import app.cloudfit.billing.application.port.output.CheckoutRequest
import app.cloudfit.billing.application.port.output.CheckoutSession
import app.cloudfit.billing.application.port.output.PaymentGateway
import app.cloudfit.billing.application.port.output.StripeAccountGateway
import app.cloudfit.billing.application.usecase.CreditBook
import app.cloudfit.billing.application.usecase.GetWalletSummaryService
import app.cloudfit.billing.application.usecase.PlanGranter
import app.cloudfit.billing.domain.BillingPolicy
import app.cloudfit.billing.domain.Currency
import app.cloudfit.billing.domain.PackProduct
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.PaymentsMode
import app.cloudfit.billing.domain.PlanProduct
import app.cloudfit.billing.domain.ProductCatalog
import app.cloudfit.shared.testing.MutableClock
import java.util.UUID

class BillingTestKit(mode: PaymentsMode = PaymentsMode.FAKE) {
    val clock = MutableClock()
    val wallet = InMemoryWalletRepository()
    val ledger = InMemoryLedgerRepository()
    val holds = InMemoryCreditHoldRepository()
    val subscriptions = InMemorySubscriptionRepository()
    val payments = InMemoryPaymentRepository()
    val customers = InMemoryBillingCustomerRepository()
    val webhookEvents = InMemoryWebhookEventRepository()
    val book = CreditBook(wallet, ledger, clock)
    val planGranter = PlanGranter(book)
    val walletSummary = GetWalletSummaryService(wallet, subscriptions, clock)
    val policy = BillingPolicy(
        mode = mode,
        catalog = ProductCatalog(
            packs = listOf(
                PackProduct("PACK_10", 10, mapOf(Currency.USD to 499L, Currency.COP to 1_990_000L)),
                PackProduct("PACK_30", 30, mapOf(Currency.USD to 1199L, Currency.COP to 4_990_000L)),
            ),
            plans = listOf(
                PlanProduct("PLUS", 40, mapOf(Currency.USD to 799L), listOf("NO_ADS", "UNLIMITED_CLOTHES")),
            ),
        ),
        adsDailyMax = 3,
        stripePlanPriceIds = mapOf("PLUS" to "price_plus"),
    )
}

class FakeGateway(
    override val provider: PaymentProvider,
    override val configured: Boolean = true,
) : PaymentGateway {
    val requests = mutableListOf<CheckoutRequest>()

    override suspend fun createCheckout(request: CheckoutRequest): CheckoutSession {
        requests += request
        return CheckoutSession("https://pay.example/${request.paymentId}", "ref_${request.paymentId}")
    }
}

class FakeStripeAccounts(override val configured: Boolean = true) : StripeAccountGateway {
    val cancelled = mutableListOf<String>()

    override suspend fun createCustomer(userId: UUID, email: String?): String = "cus_$userId"

    override suspend fun createPortalSession(customerId: String): String = "https://portal.example/$customerId"

    override suspend fun cancelSubscription(subscriptionId: String) {
        cancelled += subscriptionId
    }
}
