package app.cloudfit.billing.infrastructure.config

import app.cloudfit.billing.application.port.output.BillingUserDirectory
import app.cloudfit.billing.application.usecase.ClaimAdRewardService
import app.cloudfit.billing.application.usecase.CloseBillingAccountService
import app.cloudfit.billing.application.usecase.ConfirmCreditsService
import app.cloudfit.billing.application.usecase.CreateCheckoutService
import app.cloudfit.billing.application.usecase.CreatePortalSessionService
import app.cloudfit.billing.application.usecase.CreditBook
import app.cloudfit.billing.application.usecase.GetCatalogService
import app.cloudfit.billing.application.usecase.GetLedgerService
import app.cloudfit.billing.application.usecase.GetWalletSummaryService
import app.cloudfit.billing.application.usecase.GrantCreditsService
import app.cloudfit.billing.application.usecase.HandleStripeWebhookService
import app.cloudfit.billing.application.usecase.HandleWompiWebhookService
import app.cloudfit.billing.application.usecase.PlanGranter
import app.cloudfit.billing.application.usecase.RefundCreditsService
import app.cloudfit.billing.application.usecase.ReserveCreditsService
import app.cloudfit.billing.infrastructure.adapter.input.http.registerBillingRoutes
import app.cloudfit.billing.infrastructure.adapter.input.http.registerBillingWebhookRoutes
import app.cloudfit.billing.infrastructure.adapter.output.payment.StripePaymentGateway
import app.cloudfit.billing.infrastructure.adapter.output.payment.StripeWebhookVerifier
import app.cloudfit.billing.infrastructure.adapter.output.payment.WompiPaymentGateway
import app.cloudfit.billing.infrastructure.adapter.output.payment.WompiWebhookVerifier
import app.cloudfit.billing.infrastructure.adapter.output.persistence.PostgresBillingCustomerRepository
import app.cloudfit.billing.infrastructure.adapter.output.persistence.PostgresCreditHoldRepository
import app.cloudfit.billing.infrastructure.adapter.output.persistence.PostgresLedgerRepository
import app.cloudfit.billing.infrastructure.adapter.output.persistence.PostgresPaymentRepository
import app.cloudfit.billing.infrastructure.adapter.output.persistence.PostgresSubscriptionRepository
import app.cloudfit.billing.infrastructure.adapter.output.persistence.PostgresWalletRepository
import app.cloudfit.billing.infrastructure.adapter.output.persistence.PostgresWebhookEventRepository
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.TransactionRunner
import io.ktor.client.HttpClient
import io.ktor.server.routing.Route

class BillingComponents(
    config: BillingConfig,
    users: BillingUserDirectory,
    clock: ClockProvider,
    tx: TransactionRunner,
    http: HttpClient,
) {
    private val wallet = PostgresWalletRepository(clock)
    private val ledger = PostgresLedgerRepository()
    private val holds = PostgresCreditHoldRepository(clock)
    private val subscriptions = PostgresSubscriptionRepository(clock)
    private val payments = PostgresPaymentRepository(clock)
    private val customers = PostgresBillingCustomerRepository(clock)
    private val webhookEvents = PostgresWebhookEventRepository(clock)
    private val stripe = StripePaymentGateway(config.stripe, http)
    private val wompi = WompiPaymentGateway(config.wompi)
    private val book = CreditBook(wallet, ledger, clock)
    private val planGranter = PlanGranter(book)

    val grantCredits = GrantCreditsService(book, tx)
    val reserveCredits = ReserveCreditsService(book, holds, tx)
    val confirmCredits = ConfirmCreditsService(holds, tx)
    val refundCredits = RefundCreditsService(book, holds, ledger, tx)
    val walletSummary = GetWalletSummaryService(wallet, subscriptions, clock)
    val closeBillingAccount = CloseBillingAccountService(subscriptions, stripe)

    private val catalog = GetCatalogService(config.policy)
    private val getLedger = GetLedgerService(ledger)
    private val checkout = CreateCheckoutService(
        policy = config.policy,
        gateways = listOf(stripe, wompi),
        stripeAccounts = stripe,
        customers = customers,
        users = users,
        payments = payments,
        subscriptions = subscriptions,
        walletSummary = walletSummary,
        book = book,
        planGranter = planGranter,
        tx = tx,
        clock = clock,
    )
    private val portal = CreatePortalSessionService(stripe, customers)
    private val stripeWebhook = HandleStripeWebhookService(
        parser = StripeWebhookVerifier(config.stripe.webhookSecret, clock),
        events = webhookEvents,
        payments = payments,
        subscriptions = subscriptions,
        customers = customers,
        book = book,
        planGranter = planGranter,
        policy = config.policy,
        tx = tx,
    )
    private val wompiWebhook = HandleWompiWebhookService(
        parser = WompiWebhookVerifier(config.wompi.eventsSecret),
        events = webhookEvents,
        payments = payments,
        book = book,
        policy = config.policy,
        tx = tx,
    )
    private val adReward = ClaimAdRewardService(book, ledger, walletSummary, config.policy, tx, clock)

    fun registerRoutes(route: Route) {
        route.registerBillingRoutes(catalog, getLedger, checkout, portal, adReward)
        route.registerBillingWebhookRoutes(stripeWebhook, wompiWebhook)
    }
}

fun Route.configureBillingServiceRoutes(components: BillingComponents) {
    components.registerRoutes(this)
}
