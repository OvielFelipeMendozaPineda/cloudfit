package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.CreatePortalSessionUseCase
import app.cloudfit.billing.application.port.output.BillingCustomerRepository
import app.cloudfit.billing.application.port.output.StripeAccountGateway
import app.cloudfit.shared.application.error.NotFoundException
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import java.util.UUID

class CreatePortalSessionService(
    private val stripe: StripeAccountGateway,
    private val customers: BillingCustomerRepository,
) : CreatePortalSessionUseCase {
    override suspend fun execute(userId: UUID): String {
        if (!stripe.configured) throw ProviderNotConfiguredException("STRIPE is not configured")
        val customerId = customers.findStripeCustomer(userId) ?: throw NotFoundException("No Stripe billing account")
        return stripe.createPortalSession(customerId)
    }
}
