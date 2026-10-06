package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.CreateCheckoutCommand
import app.cloudfit.billing.application.port.input.CreateCheckoutUseCase
import app.cloudfit.billing.application.port.input.GetWalletSummaryUseCase
import app.cloudfit.billing.application.port.output.BillingCustomerRepository
import app.cloudfit.billing.application.port.output.BillingUserDirectory
import app.cloudfit.billing.application.port.output.CheckoutRequest
import app.cloudfit.billing.application.port.output.PaymentGateway
import app.cloudfit.billing.application.port.output.PaymentRepository
import app.cloudfit.billing.application.port.output.StripeAccountGateway
import app.cloudfit.billing.application.port.output.SubscriptionRepository
import app.cloudfit.billing.domain.BillingPolicy
import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.Currency
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.billing.domain.Payment
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.PaymentStatus
import app.cloudfit.billing.domain.PaymentsMode
import app.cloudfit.billing.domain.ProductKind
import app.cloudfit.billing.domain.Subscription
import app.cloudfit.billing.domain.SubscriptionStatus
import app.cloudfit.shared.application.error.ConflictException
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.TransactionRunner
import java.time.Duration
import java.util.UUID

class CreateCheckoutService(
    private val policy: BillingPolicy,
    private val gateways: List<PaymentGateway>,
    private val stripeAccounts: StripeAccountGateway,
    private val customers: BillingCustomerRepository,
    private val users: BillingUserDirectory,
    private val payments: PaymentRepository,
    private val subscriptions: SubscriptionRepository,
    private val walletSummary: GetWalletSummaryUseCase,
    private val book: CreditBook,
    private val planGranter: PlanGranter,
    private val tx: TransactionRunner,
    private val clock: ClockProvider,
) : CreateCheckoutUseCase {
    private val catalog = GetCatalogService(policy)

    override suspend fun execute(command: CreateCheckoutCommand): String {
        val pack = policy.catalog.pack(command.productCode)
        val plan = policy.catalog.plan(command.productCode)
        val kind = when {
            pack != null -> ProductKind.PACK
            plan != null -> ProductKind.PLAN
            else -> throw ValidationException("Unknown productCode ${command.productCode}")
        }
        val provider = resolveProvider(command)
        val currency = when (provider) {
            PaymentProvider.WOMPI -> Currency.COP
            PaymentProvider.STRIPE -> Currency.USD
            PaymentProvider.FAKE -> catalog.currencyFor(command.country)
        }
        if (kind == ProductKind.PLAN && currency != Currency.USD) {
            throw ValidationException("Plans are only available in USD")
        }
        val amount = (pack?.prices ?: plan!!.prices)[currency]
            ?: throw ValidationException("${command.productCode} is not sold in $currency")
        if (kind == ProductKind.PLAN && walletSummary.execute(command.userId).plan != null) {
            throw ConflictException("User already has an active plan")
        }

        val payment = Payment(
            id = UUID.randomUUID(),
            userId = command.userId,
            provider = provider,
            productCode = command.productCode,
            amount = amount,
            currency = currency,
            status = PaymentStatus.PENDING,
            providerRef = null,
        )
        if (provider == PaymentProvider.FAKE) return fakeCheckout(payment, kind)

        val gateway = gateways.firstOrNull { it.provider == provider && it.configured }
            ?: throw ProviderNotConfiguredException("$provider is not configured")
        val customerId = if (provider == PaymentProvider.STRIPE) ensureStripeCustomer(command.userId) else null
        payments.create(payment)
        val session = gateway.createCheckout(
            CheckoutRequest(
                paymentId = payment.id,
                userId = command.userId,
                kind = kind,
                productCode = command.productCode,
                description = pack?.let { "CloudFit ${it.credits} credits" }
                    ?: "CloudFit ${plan!!.code} — ${plan.monthlyCredits} credits/month",
                amount = amount,
                currency = currency,
                customerId = customerId,
                customerEmail = users.emailOf(command.userId),
            ),
        )
        session.providerRef?.let { payments.update(payment.id, PaymentStatus.PENDING, it) }
        return session.url
    }

    private fun resolveProvider(command: CreateCheckoutCommand): PaymentProvider {
        if (policy.mode == PaymentsMode.FAKE) return PaymentProvider.FAKE
        val requested = command.provider?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
            ?: return catalog.providerFor(command.country)
        val provider = PaymentProvider.entries.firstOrNull { it.name == requested }
            ?: throw ValidationException("Unknown provider $requested")
        if (provider == PaymentProvider.FAKE) throw ValidationException("FAKE provider is only available when PAYMENTS_MODE=fake")
        return provider
    }

    private suspend fun ensureStripeCustomer(userId: UUID): String =
        customers.findStripeCustomer(userId)
            ?: stripeAccounts.createCustomer(userId, users.emailOf(userId)).also { customers.saveStripeCustomer(userId, it) }

    private suspend fun fakeCheckout(payment: Payment, kind: ProductKind): String {
        val ref = payment.id.toString()
        val key = "fake:$ref"
        tx.inTransaction {
            payments.create(payment.copy(status = PaymentStatus.PAID, providerRef = key))
            when (kind) {
                ProductKind.PACK -> {
                    val pack = policy.catalog.pack(payment.productCode)!!
                    book.grant(book.lock(payment.userId), payment.userId, CreditBucket.PACK, pack.credits, LedgerReason.PURCHASE, key, ref)
                }
                ProductKind.PLAN -> {
                    val plan = policy.catalog.plan(payment.productCode)!!
                    val periodEnd = clock.now().plus(Duration.ofDays(policy.fakePlanPeriodDays))
                    subscriptions.save(
                        Subscription(
                            id = UUID.randomUUID(),
                            userId = payment.userId,
                            provider = PaymentProvider.FAKE,
                            providerSubscriptionId = key,
                            planCode = plan.code,
                            status = SubscriptionStatus.ACTIVE,
                            currentPeriodEnd = periodEnd,
                            cancelAtPeriodEnd = false,
                        ),
                    )
                    planGranter.grantPeriod(payment.userId, plan, periodEnd, key, ref)
                }
            }
        }
        return FAKE_SUCCESS_URL
    }

    companion object {
        const val FAKE_SUCCESS_URL = "/billing/success?fake=1"
    }
}
