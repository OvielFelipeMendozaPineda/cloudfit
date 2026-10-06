package app.cloudfit.billing.application.port.input

import java.util.UUID

interface CreatePortalSessionUseCase {
    suspend fun execute(userId: UUID): String
}
