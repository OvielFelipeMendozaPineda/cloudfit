package app.cloudfit.billing.application.port.input

import java.util.UUID

interface CloseBillingAccountUseCase {
    suspend fun execute(userId: UUID)
}
