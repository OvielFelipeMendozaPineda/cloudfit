package app.cloudfit.billing.application.port.input

import java.util.UUID

data class CreateCheckoutCommand(
    val userId: UUID,
    val productCode: String,
    val provider: String?,
    val country: String?,
)

interface CreateCheckoutUseCase {
    suspend fun execute(command: CreateCheckoutCommand): String
}
