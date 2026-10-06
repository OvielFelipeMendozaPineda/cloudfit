package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.HandleStripeWebhookUseCase
import app.cloudfit.billing.application.port.output.BillingCustomerRepository
import app.cloudfit.billing.application.port.output.PaymentRepository
import app.cloudfit.billing.application.port.output.StripeWebhookParser
import app.cloudfit.billing.application.port.output.SubscriptionRepository
import app.cloudfit.billing.application.port.output.WebhookEventRepository
import app.cloudfit.billing.domain.BillingPolicy
import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.PaymentStatus
import app.cloudfit.billing.domain.StripeCheckoutCompleted
import app.cloudfit.billing.domain.StripeIgnoredEvent
import app.cloudfit.billing.domain.StripeInvoicePaid
import app.cloudfit.billing.domain.StripeSubscriptionChanged
import app.cloudfit.billing.domain.Subscription
import app.cloudfit.billing.domain.SubscriptionStatus
import app.cloudfit.shared.application.port.TransactionRunner
import java.util.UUID
import org.slf4j.LoggerFactory

class HandleStripeWebhookService(
    private val parser: StripeWebhookParser,
    private val events: WebhookEventRepository,
    private val payments: PaymentRepository,
    private val subscriptions: SubscriptionRepository,
    private val customers: BillingCustomerRepository,
    private val book: CreditBook,
    private val planGranter: PlanGranter,
    private val policy: BillingPolicy,
    private val tx: TransactionRunner,
) : HandleStripeWebhookUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(payload: String, signatureHeader: String?) {
        val event = parser.parse(payload, signatureHeader)
        tx.inTransaction {
            if (!events.registerIfNew(PaymentProvider.STRIPE, event.id, event.type)) {
                log.info("Stripe event {} already processed", event.id)
                return@inTransaction
            }
            when (event) {
                is StripeCheckoutCompleted -> onCheckoutCompleted(event)
                is StripeInvoicePaid -> onInvoicePaid(event)
                is StripeSubscriptionChanged -> onSubscriptionChanged(event)
                is StripeIgnoredEvent -> Unit
            }
        }
    }

    private suspend fun onCheckoutCompleted(event: StripeCheckoutCompleted) {
        if (!event.paid) return
        val payment = event.paymentId?.toUuidOrNull()?.let { payments.findById(it) }
            ?: payments.findByProviderRef(event.sessionId)
        if (payment == null) {
            log.warn("Stripe checkout {} has no matching payment", event.sessionId)
            return
        }
        if (payment.status == PaymentStatus.PAID) return
        if (event.mode == "payment") {
            val pack = policy.catalog.pack(payment.productCode) ?: return log.warn("Unknown pack {}", payment.productCode)
            book.grant(
                buckets = book.lock(payment.userId),
                userId = payment.userId,
                bucket = CreditBucket.PACK,
                amount = pack.credits,
                reason = LedgerReason.PURCHASE,
                idempotencyKey = "stripe:checkout:${event.sessionId}",
                refId = payment.id.toString(),
            )
        } else if (event.subscriptionId != null && subscriptions.findByProviderId(event.subscriptionId) == null) {
            subscriptions.save(
                Subscription(
                    id = UUID.randomUUID(),
                    userId = payment.userId,
                    provider = PaymentProvider.STRIPE,
                    providerSubscriptionId = event.subscriptionId,
                    planCode = payment.productCode,
                    status = SubscriptionStatus.ACTIVE,
                    currentPeriodEnd = null,
                    cancelAtPeriodEnd = false,
                ),
            )
        }
        payments.update(payment.id, PaymentStatus.PAID, event.sessionId)
    }

    private suspend fun onInvoicePaid(event: StripeInvoicePaid) {
        val subscriptionId = event.subscriptionId ?: return
        val existing = subscriptions.findByProviderId(subscriptionId)
        val userId = existing?.userId ?: resolveUser(event.customerId, event.userId)
            ?: return log.warn("Stripe invoice {} has no known customer", event.invoiceId)
        val planCode = event.planCode ?: existing?.planCode ?: policy.planCodeForStripePrice(event.priceId)
            ?: return log.warn("Stripe invoice {} has no plan code", event.invoiceId)
        val plan = policy.catalog.plan(planCode) ?: return log.warn("Unknown plan {}", planCode)
        subscriptions.save(
            (existing ?: newSubscription(userId, subscriptionId, planCode)).copy(
                planCode = planCode,
                status = SubscriptionStatus.ACTIVE,
                currentPeriodEnd = event.periodEnd ?: existing?.currentPeriodEnd,
            ),
        )
        planGranter.grantPeriod(userId, plan, event.periodEnd, "stripe:invoice:${event.invoiceId}", event.invoiceId)
    }

    private suspend fun onSubscriptionChanged(event: StripeSubscriptionChanged) {
        val existing = subscriptions.findByProviderId(event.subscriptionId)
        val userId = existing?.userId ?: resolveUser(event.customerId, event.userId)
            ?: return log.warn("Stripe subscription {} has no known customer", event.subscriptionId)
        val planCode = event.planCode ?: existing?.planCode ?: policy.planCodeForStripePrice(event.priceId)
            ?: return log.warn("Stripe subscription {} has no plan code", event.subscriptionId)
        val status = if (event.deleted) SubscriptionStatus.CANCELED else SubscriptionStatus.fromStripe(event.status)
        subscriptions.save(
            (existing ?: newSubscription(userId, event.subscriptionId, planCode)).copy(
                planCode = planCode,
                status = status,
                currentPeriodEnd = event.currentPeriodEnd ?: existing?.currentPeriodEnd,
                cancelAtPeriodEnd = event.cancelAtPeriodEnd,
            ),
        )
        if (event.deleted) {
            planGranter.expireAll(userId, "stripe:subscription:${event.subscriptionId}:deleted", event.subscriptionId)
        }
    }

    private suspend fun resolveUser(customerId: String?, metadataUserId: String?): UUID? =
        customerId?.let { customers.findUserByStripeCustomer(it) } ?: metadataUserId?.toUuidOrNull()

    private fun newSubscription(userId: UUID, providerId: String, planCode: String) = Subscription(
        id = UUID.randomUUID(),
        userId = userId,
        provider = PaymentProvider.STRIPE,
        providerSubscriptionId = providerId,
        planCode = planCode,
        status = SubscriptionStatus.INCOMPLETE,
        currentPeriodEnd = null,
        cancelAtPeriodEnd = false,
    )
}

internal fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()
