package app.cloudfit.accounts.application.port.input

import app.cloudfit.accounts.domain.AccountProfile
import app.cloudfit.accounts.domain.Me
import java.util.UUID

data class UpdateMeCommand(
    val userId: UUID,
    val displayName: String?,
    val locale: String?,
)

interface GetMeUseCase {
    suspend fun execute(userId: UUID): Me
}

interface UpdateMeUseCase {
    suspend fun execute(command: UpdateMeCommand): Me
}

interface DeleteAccountUseCase {
    suspend fun execute(userId: UUID)
}

interface GetAccountProfileUseCase {
    suspend fun execute(userId: UUID): AccountProfile?
}
