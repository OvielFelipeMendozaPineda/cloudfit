package app.cloudfit.billing.application.port.input

import java.util.UUID

interface ClaimAdRewardUseCase {
    suspend fun execute(userId: UUID, placement: String): Int
}
