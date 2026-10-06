package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.CloseBillingAccountUseCase
import app.cloudfit.billing.application.port.output.StripeAccountGateway
import app.cloudfit.billing.application.port.output.SubscriptionRepository
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.SubscriptionStatus
import java.util.UUID
import org.slf4j.LoggerFactory

class CloseBillingAccountService(
    private val subscriptions: SubscriptionRepository,
    private val stripe: StripeAccountGateway,
) : CloseBillingAccountUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(userId: UUID) {
        subscriptions.listByUser(userId)
            .filter { it.provider == PaymentProvider.STRIPE && it.status != SubscriptionStatus.CANCELED }
            .mapNotNull { it.providerSubscriptionId }
            .forEach { subscriptionId ->
                runCatching { stripe.cancelSubscription(subscriptionId) }
                    .onFailure { log.error("Could not cancel Stripe subscription {}: {}", subscriptionId, it.message) }
            }
    }
}
